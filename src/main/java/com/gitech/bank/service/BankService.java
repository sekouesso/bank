package com.gitech.bank.service;

import com.gitech.bank.dto.AccountDTO;
import com.gitech.bank.dto.ClientDTO;
import com.gitech.bank.dto.VirementDTO;
import com.gitech.bank.entity.Account;
import com.gitech.bank.entity.Client;
import com.gitech.bank.exception.DuplicateResourceException;
import com.gitech.bank.exception.ResourceNotFoundException;
import com.gitech.bank.exception.SoldeInsuffisantException;
import com.gitech.bank.repository.AccountRepository;
import com.gitech.bank.repository.ClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class BankService {

    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;

    public BankService(ClientRepository clientRepository, AccountRepository accountRepository) {
        this.clientRepository = clientRepository;
        this.accountRepository = accountRepository;
    }

    // =========================================================================
    //  CLIENTS
    // =========================================================================

    public Page<ClientDTO> getClients(String query, Pageable pageable) {
        Page<Client> clients = (query != null && !query.isBlank())
                ? clientRepository.findByNomContainingIgnoreCase(query, pageable)
                : clientRepository.findAll(pageable);
        return clients.map(this::toClientDTO);
    }

    public ClientDTO getClientById(Long id) {
        return clientRepository.findById(id)
                .map(this::toClientDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", id));
    }

    public ClientDTO saveClient(ClientDTO dto) {
        Client client = Client.builder()
                .nom(dto.getNom())
                .telephone(dto.getTelephone())
                .email(dto.getEmail())
                .statut(dto.getStatut())
                .build();
        Client saved = clientRepository.save(client);
        return toClientDTO(saved);
    }

    public ClientDTO updateClient(Long id, ClientDTO dto) {
        Client existing = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", id));
        existing.setNom(dto.getNom());
        existing.setEmail(dto.getEmail());
        existing.setTelephone(dto.getTelephone());
        existing.setStatut(dto.getStatut());
        return toClientDTO(clientRepository.save(existing));
    }

    public void deleteClient(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Client", "id", id);
        }
        clientRepository.deleteById(id);
    }

    // =========================================================================
    //  COMPTES
    // =========================================================================

    public Page<AccountDTO> getAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable).map(this::toAccountDTO);
    }

    public AccountDTO getAccountById(Long id) {
        return accountRepository.findById(id)
                .map(this::toAccountDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Compte", "id", id));
    }

    public List<AccountDTO> getAccountsByClientId(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client", "id", clientId);
        }
        return accountRepository.findByClientId(clientId).stream()
                .map(this::toAccountDTO)
                .toList();
    }

    public AccountDTO createAccount(AccountDTO dto) {
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", dto.getClientId()));

        if (accountRepository.existsByAccountNumber(dto.getAccountNumber())) {
            throw new DuplicateResourceException("Ce numéro de compte existe déjà : " + dto.getAccountNumber());
        }

        Account account = Account.builder()
                .accountNumber(dto.getAccountNumber())
                .balance(dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO)
                .type(dto.getType())
                .client(client)
                .build();

        return toAccountDTO(accountRepository.save(account));
    }

    /**
     * Le numéro de compte (`accountNumber`) n'est volontairement JAMAIS
     * modifié ici, même s'il est présent dans le DTO reçu : c'est un
     * identifiant fonctionnel stable (comme un IBAN), au même titre que
     * l'`id` technique. Seuls le type, le solde et éventuellement le
     * client propriétaire peuvent être mis à jour.
     */
    public AccountDTO updateAccount(Long id, AccountDTO dto) {
        Account existing = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte", "id", id));

        if (dto.getClientId() != null && !dto.getClientId().equals(existing.getClient().getId())) {
            Client nouveauClient = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client", "id", dto.getClientId()));
            existing.setClient(nouveauClient);
        }

        existing.setType(dto.getType());
        existing.setBalance(dto.getBalance() != null ? dto.getBalance() : existing.getBalance());

        return toAccountDTO(accountRepository.save(existing));
    }

    public void deleteAccount(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new ResourceNotFoundException("Compte", "id", id);
        }
        accountRepository.deleteById(id);
    }

    // =========================================================================
    //  OPÉRATIONS BANCAIRES (dépôt / retrait / virement)
    // =========================================================================

    public AccountDTO depot(Long id, BigDecimal montant) {
        Account compte = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte", "id", id));

        compte.setBalance(compte.getBalance().add(montant));
        return toAccountDTO(accountRepository.save(compte));
    }

    public AccountDTO retrait(Long id, BigDecimal montant) {
        Account compte = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte", "id", id));

        verifierSoldeSuffisant(compte, montant);
        compte.setBalance(compte.getBalance().subtract(montant));
        return toAccountDTO(accountRepository.save(compte));
    }

    /**
     * @Transactional (posé au niveau de la classe) garantit qu'un virement
     * est ATOMIQUE : si l'écriture sur le compte destination échoue pour
     * une raison quelconque, le débit déjà effectué sur le compte source
     * est automatiquement annulé (rollback) — on ne se retrouve jamais
     * avec de l'argent "débité mais jamais crédité".
     */
    public AccountDTO virement(Long compteSourceId, VirementDTO dto) {
        if (compteSourceId.equals(dto.getCompteDestinationId())) {
            throw new IllegalArgumentException("Le compte source et le compte destination doivent être différents");
        }

        Account source = accountRepository.findById(compteSourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte", "id", compteSourceId));
        Account destination = accountRepository.findById(dto.getCompteDestinationId())
                .orElseThrow(() -> new ResourceNotFoundException("Compte", "id", dto.getCompteDestinationId()));

        verifierSoldeSuffisant(source, dto.getMontant());

        source.setBalance(source.getBalance().subtract(dto.getMontant()));
        destination.setBalance(destination.getBalance().add(dto.getMontant()));

        accountRepository.save(destination);
        return toAccountDTO(accountRepository.save(source));
    }

    private void verifierSoldeSuffisant(Account compte, BigDecimal montant) {
        if (compte.getBalance().compareTo(montant) < 0) {
            throw new SoldeInsuffisantException(
                    "Solde insuffisant sur le compte " + compte.getAccountNumber()
                            + " (solde actuel : " + compte.getBalance() + ")");
        }
    }

    // =========================================================================
    //  MAPPING ENTITÉ ↔ DTO
    // =========================================================================

    private ClientDTO toClientDTO(Client c) {
        return new ClientDTO(c.getId(), c.getNom(), c.getEmail(), c.getTelephone(), c.getStatut());
    }

    private AccountDTO toAccountDTO(Account a) {
        return new AccountDTO(
                a.getId(),
                a.getClient() != null ? a.getClient().getId() : null,
                a.getAccountNumber(),
                a.getBalance(),
                a.getType());
    }
}
