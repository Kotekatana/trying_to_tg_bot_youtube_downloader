package com.tgbots.prodavan.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Data
@Builder
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity(name="usersDataTable")
public class User {
    @Id
    private long chatId;
    private String firstName;
    private String lastName;
    private String userName;
}
