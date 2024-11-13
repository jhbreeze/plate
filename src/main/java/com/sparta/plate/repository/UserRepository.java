package com.sparta.plate.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sparta.plate.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	boolean existsByLoginId(String loginId); // 존재 여부 확인은 exists

	boolean existsByEmail(String email);

	User saveAndFlush(User user);

	Optional<User> findByLoginId(String loginId);

	Optional<User> findByIdAndIsDeletedFalse(Long id);

	Page<User> findByIsDeletedFalse(Pageable pageable);

	Page<User> findByLoginIdContainingAndIsDeletedFalse(Pageable pageable, String search);
}
