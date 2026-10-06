package com.gitech.bank.dto;

import com.gitech.bank.entity.TypeCompte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO du compte, utilisé à la fois en lecture (GET) et désormais en écriture
 * (POST/PUT, voir BankController). Le champ `id` est ignoré à la création
 * (généré par la base) et `accountNumber` n'est modifiable qu'à la création
 * (voir BankService.updateAccount : il n'est volontairement jamais réécrit
 * lors d'une mise à jour, à la manière d'un IBAN qui ne change pas).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDTO {

    private Long id;

    @NotNull(message = "L'identifiant du client est obligatoire")
    private Long clientId;

    @NotBlank(message = "Le numéro de compte est obligatoire")
    private String accountNumber;

    @PositiveOrZero(message = "Le solde ne peut pas être négatif")
    private BigDecimal balance;

    @NotNull(message = "Le type de compte est obligatoire")
    private TypeCompte type;
}
