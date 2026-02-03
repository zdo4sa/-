package com.example.twentysix.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.twentysix.entity.SurveyResponse;

@Repository
public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long> {
	// 特定の予約IDに対して既に回答があるか確認する
	Optional<SurveyResponse> findByReservationId(Long reservationId);

	List<SurveyResponse> findAllByOrderByIdDesc();
}