package org.example.sandbox_backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "profile_info")
public class ProfileInfo {

    @Id
    @Column(name = "user_id")
    private String userId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false)
    private UserInfo userInfo;

    private String fullName;
    private String email;
    private String bio;

    @Lob
    private byte[] profileImage;
}
