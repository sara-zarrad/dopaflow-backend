package crm.dopaflow_backend.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    @Getter
    private Long id;

    @Getter
    @Setter
    @Column(unique = true, nullable = false)
    private String username;

    @Getter
    @Setter
    @Column(unique = true, nullable = false)
    private String email;

    @Getter
    @Setter
    @Column(nullable = false)
    private String password;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Getter
    @Setter
    @Column(nullable = false)
    private Date birthdate;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    private StatutUser status;

    @Getter
    @Setter
    @Column(nullable = false)
    private Date lastLogin;

    @Getter
    @Setter
    @Column(nullable = false)
    private Boolean verified;

    @Getter
    @Setter
    private String verificationToken;

    @Getter
    @Setter
    @Column(name = "two_factor_enabled")
    private boolean twoFactorEnabled;

    @Getter
    @Setter
    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    @Getter
    @Setter
    @Column(name = "profile_photo_url")
    private String profilePhotoUrl; // New field for profile photo URL

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    @Getter
    @Setter
    private List<LoginHistory> loginHistory = new ArrayList<>();



    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role.name()));
    }


    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.verified;
    }

    public void addLoginHistory(LoginHistory history) {
        loginHistory.add(history);
        history.setUser(this);
    }


    public boolean getTwoFactorEnabled() {
        return false;
    }

    public boolean isTwoFactorEnabled() {
        return false;
    }

    public String getVerificationToken() {
        return null;
    }
}

