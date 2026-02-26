# Design System

## 1. 개요

**IBM Carbon Design System**을 기반으로 컴포넌트·레이아웃·색상·타이포그래피 등 시각 체계를 통일한다.

> **IBM Carbon Design System:** https://carbondesignsystem.com

컴포넌트별 상세 가이드라인은 공식 문서를 참고한다.

---

## 2. 레이아웃

### 2x Grid (Mini Unit)

Carbon의 **Mini Unit(8px)** 기반으로 모든 간격과 크기를 설정한다. 미세 조정이 필요하면 4px 단위를 허용한다.

| Carbon Spacing Token | 값 | 용도 |
|------|-----|------|
| `$spacing-03` | 8px | 관련 요소 간 밀착 간격 |
| `$spacing-05` | 16px | 기본 컴포넌트 간 간격 |
| `$spacing-06` | 24px | 섹션 간 구분 |
| `$spacing-07` | 32px | 주요 영역 간 구분 |

> 참고: https://carbondesignsystem.com/elements/2x-grid/overview/

---

## 3. 색상

### 3.1 시맨틱 컬러 토큰

Carbon의 **Design Token** 체계를 따른다. 하드코딩된 색상값을 직접 사용하지 않는다.

| Carbon Token | 용도 |
|------|------|
| `$interactive` | 주요 액션 (링크, 버튼) |
| `$text-primary` / `$text-secondary` | 본문 텍스트 계층 |
| `$support-error` | 오류 상태 |
| `$support-success` | 성공 상태 |
| `$support-warning` | 경고 상태 |
| `$support-info` | 정보 상태 |
| `$background` | 페이지 배경 |
| `$layer-01` / `$layer-02` | 카드·시트 배경 (레이어 계층) |
| `$border-subtle` / `$border-strong` | 테두리 계층 |
| `$icon-primary` / `$icon-secondary` | 아이콘 계층 |

> 참고: https://carbondesignsystem.com/elements/color/tokens/

### 3.2 대비 기준

Carbon은 **WCAG 2.1 AA** 기준을 따른다.

- 일반 텍스트: 최소 **4.5:1** 대비
- 대형 텍스트(18px 이상 또는 14px bold): 최소 **3:1** 대비
- 색상만으로 정보를 전달하지 않는다

---

## 4. 타이포그래피

### 4.1 Type Scale

Carbon의 **Type Scale**을 사용한다. 절대 크기 대신 **역할(role)** 기반으로 서체를 지정한다.

| Carbon Type Token | 용도 |
|------|------|
| `$heading-03` ~ `$heading-01` | 페이지·섹션 제목 |
| `$body-02` / `$body-01` | 본문 텍스트 |
| `$label-01` | 입력 필드 레이블, 보조 텍스트 |
| `$helper-text-01` | 도움말 텍스트 |
| `$code-01` / `$code-02` | 코드 텍스트 |

> 참고: https://carbondesignsystem.com/elements/typography/type-sets/

### 4.2 서체

프로젝트는 **IBM Plex Sans KR**을 기본 서체로 사용한다.

---

## 5. 컴포넌트

### 5.1 Button

Carbon Button의 계층 구조를 따른다.

| 종류 | Carbon Class | 용도 |
|------|--------|------|
| **Primary** | `.cds--btn--primary` | 화면의 주요 액션 (1개) |
| **Secondary** | `.cds--btn--secondary` | 보조 액션 |
| **Tertiary** | `.cds--btn--tertiary` | 부가 액션 |
| **Ghost** | `.cds--btn--ghost` | 최소 강조 액션 |
| **Danger** | `.cds--btn--danger` | 삭제·파괴적 액션 |

> 참고: https://carbondesignsystem.com/components/button/usage/

### 5.2 DataTable

Carbon DataTable 패턴을 따른다.

| 규칙 | 설명 |
|------|------|
| 숫자·날짜 우측 정렬 | Carbon DataTable 정렬 가이드 준수 |
| 행 높이 | Compact(24px), Short(32px), Default(48px), Tall(64px) |
| 정렬 | 컬럼 헤더 클릭 시 asc → desc → none 순환 |
| 선택 | 체크박스 기반 multi-select, 헤더 전체 선택 지원 |

> 참고: https://carbondesignsystem.com/components/data-table/usage/

### 5.3 Pagination

Carbon Pagination 패턴을 따른다.

| 규칙 | 설명 |
|------|------|
| 총 건수 표시 | `{start}–{end} of {total}` 형식 |
| 페이지당 건수 선택 | 드롭다운으로 건수 변경 |
| 네비게이션 | 이전/다음 + 페이지 번호 |

> 참고: https://carbondesignsystem.com/components/pagination/usage/

### 5.4 Modal

Carbon Modal 가이드라인을 따른다.

| 규칙 | 설명 |
|------|------|
| 포커스 트랩 | 모달 내부에서만 Tab 키 순환 |
| 포커스 복원 | 모달 닫힘 시 트리거 요소로 포커스 복귀 |
| Escape 키 | Escape 키로 모달 닫기 지원 |
| 오버레이 | 배경 오버레이 클릭 시 닫기 허용 |

> 참고: https://carbondesignsystem.com/components/modal/usage/

### 5.5 Notification (Toast)

Carbon Inline/Toast Notification 패턴을 따른다.

| 종류 | 용도 |
|------|------|
| **Success** | 작업 완료 |
| **Error** | 오류 발생 |
| **Warning** | 주의 필요 |
| **Info** | 일반 안내 |

> 참고: https://carbondesignsystem.com/components/notification/usage/

---

## 6. 접근성

Carbon은 **WCAG 2.1 AA** 준수를 기본으로 한다. 프로젝트에서 준수할 Carbon 접근성 규칙:

| 규칙 | 설명 |
|------|------|
| rem 기반 폰트 크기 | px 고정값 대신 rem 단위를 사용한다 |
| 포커스 링 유지 | `outline: none`으로 포커스 링을 제거하지 않는다 |
| 모달 포커스 트랩 | 모달 내부에서만 Tab이 순환하도록 트랩 적용 |
| `prefers-reduced-motion` 존중 | 모션 감소 설정 시 애니메이션 최소화 |
| ARIA 속성 | 인터랙티브 컴포넌트에 적절한 ARIA role·label 부여 |

> 참고: https://carbondesignsystem.com/guidelines/accessibility/overview/

---

*Last updated: 2026-02-27*
