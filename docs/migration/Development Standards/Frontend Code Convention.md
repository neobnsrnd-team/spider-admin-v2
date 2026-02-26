# Frontend Code Convention

## 1. 개요

프론트엔드 코드 품질과 일관성을 강제하는 규칙을 정의한다. **ESLint**를 중심으로 JavaScript·HTML·CSS 코드 표준을 자동화한다.

> **ESLint:** https://eslint.org

---

## 2. Tailwind CSS

**버전:** 3.4.x | **설정 파일:** `tailwind.config.js`

유틸리티 클래스 기반 스타일링 시스템이다. 모든 스타일은 Tailwind 유틸리티 클래스 또는 `app.css`(`@apply` 기반)로 작성한다. Inline `style=""` 및 Raw hex 색상값 직접 작성은 금지한다.

**빌드 방식:**

전 환경에서 PostCSS 빌드만 사용한다. CDN 방식은 Security.md §8.1의 CSP 정책(`style-src 'self'`)에 의해 차단되므로 로컬 개발 환경에서도 사용하지 않는다.

| 방식 | 환경 | 명령어 |
|------|------|--------|
| PostCSS 빌드 | 전 환경 (로컬·CI·운영) | 로컬: `npm run css:watch` / CI·운영: `npm run css:build` |

---

## 3. jQuery

**버전:** 3.7.x

DOM 조작, AJAX 통신, 이벤트 처리에 사용한다. `fetch()` 대신 `$.ajax()`를 사용하여 CSRF 토큰 처리와 에러 핸들링을 일관되게 적용한다. 이벤트는 탭 재진입 안전성을 위해 `.off().on()` 패턴을 사용한다.

공통 라이브러리는 `layout.html`에서 **한 번만** 로드한다.

---

## 4. ESLint

**버전:** 9.x | **설정 파일:** `eslint.config.js`

JavaScript 코드 품질을 강제한다. CI 빌드를 차단한다.

```js
// eslint.config.js
import globals from 'globals';

export default [{
  files: ['../resources/static/js/**/*.js'],   // src/main/frontend/ 기준 상대 경로
  languageOptions: {
    globals: {
      ...globals.browser,
      ...globals.jquery,
    },
  },
  rules: {
    'no-var':                'error',
    'no-implicit-globals':   'error',
    'eqeqeq':               'error',
    'prefer-const':          'warn',
    'no-restricted-globals': ['error', { name: 'fetch', message: 'Use $.ajax() instead.' }],
    'no-restricted-syntax':  ['warn',
      { selector: "CallExpression[callee.property.name='addEventListener']",
        message: 'Use jQuery .off().on() instead.' }
    ],
  },
}];
```

---

## 5. Husky & HTMLHint

**Husky 버전:** 9.x | **HTMLHint 버전:** 1.x

Husky는 pre-commit 훅으로 금지 패턴을 커밋 시점에 차단한다. HTMLHint는 Thymeleaf 템플릿의 HTML 구조를 검사한다.

---

## 6. 금지 사항

### 6.1 HTML / 템플릿

| 금지 항목 | 이유 | 대안 |
|----------|------|------|
| Inline `style=""` 작성 | 스타일이 분산되어 유지보수가 어려워진다 | Tailwind 유틸리티 클래스 또는 `app.css`를 사용한다 |
| `th:insert` 사용 | 불필요한 wrapper 태그가 생성된다 | `th:replace`를 사용한다 |
| `th:object` / `th:field` / `th:with` 사용 | JS 컴포넌트 방식과 충돌한다 | fragment 파라미터 또는 JS 변수로 대체한다 |
| Page fragment에 완전한 HTML 문서 구조 작성 | `#dynamic-content` 주입 시 DOM 구조가 깨진다 | `<div class="content-inner">`로 시작하는 fragment 형식으로 작성한다 |
| Raw hex 색상값 HTML에 직접 작성 | 색상 토큰 일관성이 깨진다 | `tailwind.config.js`의 커스텀 색상 토큰을 사용한다 |
| CDN 중복 import | 리소스 낭비 및 충돌 위험이 있다 | jQuery 등 공통 라이브러리는 `home.html`에서만 로드한다 |
| `th:utext` 무분별 사용 | XSS 취약점이 발생한다 | HTML 마크업이 포함된 데이터에만 허용하고, 기본은 `th:text`를 사용한다 |

### 6.2 JavaScript

| 금지 항목 | 이유 | 대안 |
|----------|------|------|
| `var` 사용 | 호이스팅으로 예측하기 어려운 버그가 발생한다 | `let` / `const`를 사용한다 |
| 암묵적 전역 변수 선언 | 탭 간 상태 오염이 발생한다 | `window.{Domain}Page` 네임스페이스에 명시적으로 등록한다 |
| `==` 비교 연산자 | 타입 강제 변환으로 의도치 않은 결과가 발생한다 | `===`를 사용한다 |
| `fetch()` 사용 | CSRF 처리·에러 핸들링 방식이 달라 일관성이 깨진다 | `$.ajax()`로 통일한다 |
| `addEventListener` 직접 사용 | 탭 재진입 시 이벤트가 중복 등록된다 | jQuery `.off().on()` 패턴을 사용한다 |
| 크레덴셜 하드코딩 (Playwright) | 소스 코드에 인증 정보가 노출된다 | 환경변수(`process.env.*`) 또는 `.env` 파일을 사용한다 |

### 6.3 자동 강제 항목

| 금지 항목 | 도구 | 강제 수준 |
|----------|------|-----------|
| Page fragment에 `<html>` 태그 포함 | Pre-commit | 커밋 차단 |
| `th:insert` 사용 | Pre-commit | 커밋 차단 |
| `th:object` / `th:field` / `th:with` 사용 | Pre-commit | 커밋 차단 |
| Inline `style=""` 작성 | HTMLHint + Pre-commit | 커밋 차단 |
| `var` 사용 | ESLint `no-var` | 빌드 실패 |
| 암묵적 전역 변수 선언 | ESLint `no-implicit-globals` | 빌드 실패 |
| `==` 비교 연산자 | ESLint `eqeqeq` | 빌드 실패 |
| `fetch()` 사용 | ESLint `no-restricted-globals` | 빌드 실패 |
| 크레덴셜 하드코딩 | ESLint `no-secrets` | 빌드 실패 |

---

## 7. 체크리스트

```
□ 1. npm run lint (ESLint + HTMLHint) 통과 확인
□ 2. Inline style="" 미사용
□ 3. th:insert, th:object, th:field, th:with 미사용
□ 4. Raw hex 색상값 직접 작성 없음
□ 5. 공통 라이브러리 중복 import 없음
□ 6. var 미사용, === 비교 사용
□ 7. window.{Domain}Page 네임스페이스 패턴 준수
□ 8. $.ajax() 사용 (fetch 금지)
```

---

*Last updated: 2026-02-26*
