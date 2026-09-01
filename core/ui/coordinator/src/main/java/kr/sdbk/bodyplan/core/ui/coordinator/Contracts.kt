package kr.sdbk.bodyplan.core.ui.coordinator

/** 화면이 그리는 값의 전부. 데이터 클래스로 구현하고 기본값을 초기 상태로 쓴다. */
interface State

/** 화면에서 올라오는 사용자 입력. */
interface Intent

/** 한 번만 소비되는 화면 밖 동작 — 이동, 스낵바, 시스템 호출. */
interface Effect
