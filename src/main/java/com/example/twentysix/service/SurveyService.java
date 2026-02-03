package com.example.twentysix.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.twentysix.entity.Reservation;
import com.example.twentysix.entity.SurveyResponse;
import com.example.twentysix.repository.ReservationRepository;
import com.example.twentysix.repository.SurveyResponseRepository;

@Service
public class SurveyService {
	private final SurveyResponseRepository surveyResponseRepository;
	private final ReservationRepository reservationRepository;

	public SurveyService(SurveyResponseRepository surveyResponseRepository,
			ReservationRepository reservationRepository) {
		this.surveyResponseRepository = surveyResponseRepository;
		this.reservationRepository = reservationRepository;
	}

	// アンケート回答の保存
	@Transactional
	public void saveSurveyResponse(Long reservationId, Integer staffRating, Integer serviceRating, String comment) {
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new IllegalArgumentException("予約が見つかりません。"));

		// 回答済みかチェック
		if (surveyResponseRepository.findByReservationId(reservationId).isPresent()) {
			throw new IllegalStateException("この予約には既にご回答済みです。");
		}

		// 予約日時が過去かチェック
		LocalDateTime reservationDateTime = LocalDateTime.of(reservation.getRecordDate(), reservation.getTimeSlot());
		if (reservationDateTime.isAfter(LocalDateTime.now())) {
			throw new IllegalStateException("予約日時が過ぎていないため、回答できません。");
		}

		// 保存実行
		SurveyResponse response = new SurveyResponse();
		response.setReservation(reservation);
		response.setStaffRating(staffRating);
		response.setServiceRating(serviceRating);
		response.setComment(comment);
		surveyResponseRepository.save(response);
	}
}