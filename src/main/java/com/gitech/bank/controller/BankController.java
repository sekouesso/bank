package com.gitech.bank.controller;

import com.gitech.bank.dto.AccountDTO;
import com.gitech.bank.dto.ClientDTO;
import com.gitech.bank.dto.MontantOperationDTO;
import com.gitech.bank.dto.VirementDTO;
import com.gitech.bank.service.BankService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    // =========================================================================
    //  CLIENTS
    //  Consultation : tout utilisateur authentifié (ADMIN ou USER).
    //  Écriture (create/update/delete) : réservée aux ADMIN via @PreAuthorize.
    //  Rappel : @PreAuthorize n'est actif QUE grâce à @EnableMethodSecurity
    //  déclaré dans SecurityConfig ; il s'agit d'un SECOND niveau de
    //  protection, en plus de celle déjà posée au niveau des URL dans
    //  SecurityConfig.securityFilterChain() (.anyRequest().authenticated()).
    // =========================================================================

    @GetMapping("/clients")
    public ResponseEntity<Page<ClientDTO>> getClients(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(bankService.getClients(search, pageable));
    }

    @GetMapping("/clients/{id}")
    public ResponseEntity<ClientDTO> getClient(@PathVariable Long id) {
        return ResponseEntity.ok(bankService.getClientById(id));
    }

    @PostMapping("/clients")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientDTO> createClient(@Valid @RequestBody ClientDTO dto) {
        return new ResponseEntity<>(bankService.saveClient(dto), HttpStatus.CREATED);
    }

    @PutMapping("/clients/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientDTO> updateClient(@PathVariable Long id, @Valid @RequestBody ClientDTO dto) {
        return ResponseEntity.ok(bankService.updateClient(id, dto));
    }

    @DeleteMapping("/clients/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        bankService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    //  COMPTES
    // =========================================================================

    @GetMapping("/accounts")
    public ResponseEntity<Page<AccountDTO>> getAccounts(Pageable pageable) {
        return ResponseEntity.ok(bankService.getAccounts(pageable));
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountDTO> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(bankService.getAccountById(id));
    }

    @GetMapping("/clients/{clientId}/accounts")
    public ResponseEntity<List<AccountDTO>> getClientAccounts(@PathVariable Long clientId) {
        return ResponseEntity.ok(bankService.getAccountsByClientId(clientId));
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountDTO> createAccount(@Valid @RequestBody AccountDTO dto) {
        return new ResponseEntity<>(bankService.createAccount(dto), HttpStatus.CREATED);
    }

    @PutMapping("/accounts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountDTO> updateAccount(@PathVariable Long id, @Valid @RequestBody AccountDTO dto) {
        return ResponseEntity.ok(bankService.updateAccount(id, dto));
    }

    @DeleteMapping("/accounts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        bankService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    //  OPÉRATIONS BANCAIRES
    //  Ouvertes à tout utilisateur authentifié (pas de restriction de rôle) :
    //  dans une vraie application, on vérifierait en plus que l'utilisateur
    //  connecté est bien le propriétaire du compte (hors périmètre de ce
    //  cours, qui se concentre sur l'authentification JWT elle-même).
    // =========================================================================

    @PostMapping("/accounts/{id}/depot")
    public ResponseEntity<AccountDTO> depot(@PathVariable Long id, @Valid @RequestBody MontantOperationDTO dto) {
        return ResponseEntity.ok(bankService.depot(id, dto.getMontant()));
    }

    @PostMapping("/accounts/{id}/retrait")
    public ResponseEntity<AccountDTO> retrait(@PathVariable Long id, @Valid @RequestBody MontantOperationDTO dto) {
        return ResponseEntity.ok(bankService.retrait(id, dto.getMontant()));
    }

    @PostMapping("/accounts/{id}/virement")
    public ResponseEntity<AccountDTO> virement(@PathVariable Long id, @Valid @RequestBody VirementDTO dto) {
        return ResponseEntity.ok(bankService.virement(id, dto));
    }
}
