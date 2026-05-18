package com.example.geoserver.entity

import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "login", unique = true, nullable = false)
    private var login: String = "",

    @Column(name = "full_name", nullable = false)
    var fullName: String = "",

    @Column(nullable = false)
    private var password: String = "",

    @Column(nullable = false)
    var role: String = "",
    
    @Column(nullable = true)
    var position: String? = null

) : UserDetails {

    fun updateLogin(newLogin: String) {
        login = newLogin.trim()
    }

    fun updateFullName(newFullName: String) {
        fullName = newFullName.trim()
    }

    fun updatePassword(newPassword: String) {
        password = newPassword
    }

    fun updatePosition(newPosition: String) {
        position = newPosition
    }

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority(role))
    }

    override fun getPassword(): String = password
    override fun getUsername(): String = login
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id && login == other.login
    }

    override fun hashCode(): Int {
        return 31 * id.hashCode() + login.hashCode()
    }

    override fun toString(): String {
        return "User(id=$id, login='$login', fullName='$fullName', role='$role')"
    }
}