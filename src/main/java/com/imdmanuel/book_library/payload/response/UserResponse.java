package com.imdmanuel.book_library.payload.response;

import java.util.Date;
import java.util.Set;

import com.imdmanuel.book_library.models.Role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Set<Role> roles;
    private int strikes;
    private Date suspensionUntil;
    private boolean isSuspended;
}
