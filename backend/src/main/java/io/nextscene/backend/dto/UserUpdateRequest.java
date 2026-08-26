package io.nextscene.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Edição de perfil. Todo campo é opcional — o que vier nulo ou em branco não é
 * tocado —, mas o que vier <b>precisa</b> ser válido.
 * <p>
 * Antes este record era três {@code String} cruas, sem nenhuma anotação: o
 * cadastro exigia senha de 6 caracteres e a edição aceitava uma de 1, o que
 * tornava a política de senha contornável por outro endpoint. Um e-mail
 * inválido gravado aqui também deixava o usuário sem conseguir entrar.
 *
 * @param currentPassword obrigatório para trocar a senha — ver
 *                        {@code UserService.updateProfile}.
 */
public record UserUpdateRequest(
        @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
        String name,

        @Email(message = "Email inválido")
        @Size(max = 255)
        String email,

        @Size(min = 6, message = "Senha deve ter pelo menos 6 caracteres")
        String password,

        String currentPassword
) {}
