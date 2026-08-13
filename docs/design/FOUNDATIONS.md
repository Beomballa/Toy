# NOREN Foundations

## Color

| Role | Token | Value | Use |
| --- | --- | --- | --- |
| Page | `--noren-page` | `#F8F7F3` | 앱 배경 |
| Surface | `--noren-surface` | `#FFFFFF` | 카드, 모달, 입력 |
| Ink | `--noren-ink` | `#1E2722` | 제목, 본문 |
| Muted | `--noren-muted` | `#5F6B65` | 보조 정보 |
| Border | `--noren-border` | `#E5E3DC` | 구분선 |
| Sage | `--noren-sage` | `#5E735F` | 기본 강조와 포커스 |
| Sage soft | `--noren-sage-soft` | `#E7EEE6` | 선택 상태 배경 |
| Terracotta | `--noren-terracotta` | `#B65A3F` | 긴급도와 주의 행동 |
| Terracotta soft | `--noren-terracotta-soft` | `#F4E6DE` | 경고 배경 |

흰색 위 세이지와 테라코타의 대비는 일반 텍스트 AA 기준을 충족하도록 정했다. 팔레트 방향은 [Happy Hues Palette 5](https://www.happyhues.co/palettes/5)를 참고했고, 실제 대비 판단은 [Adobe Color Contrast Analyzer](https://color.adobe.com/create/color-contrast-analyzer) 기준으로 검증한다.

## Typography

- 기본 글꼴: Pretendard Variable, Pretendard, sans-serif
- 본문: 14px / 1.6
- 보조 정보: 12px / 1.5
- 주요 탐색: 14px / 600
- 섹션 제목: 20px / 700
- 페이지 제목: `clamp(28px, 4vw, 42px)` / 750
- 숫자 가격: tabular numbers 사용

한 화면에서 4단계보다 많은 폰트 크기를 사용하지 않는다. 대문자 영문 eyebrow는 11px, 0.08em 자간으로 제한한다.

## Layout

- 최대 콘텐츠 폭: 1,200px
- 데스크톱 좌우 여백: 40px
- 태블릿 좌우 여백: 24px
- 모바일 좌우 여백: 16px
- 기본 간격 배수: 4, 8, 12, 16, 24, 32, 48, 64, 96
- 카드 반경: 12px
- 모달 반경: 18px
- 입력 및 버튼 높이: 44px, 주요 행동 48px

## Motion

상태 전환은 160~220ms 범위의 opacity와 transform만 사용한다. `prefers-reduced-motion: reduce`에서는 자동 배너, 스크롤 애니메이션과 장식 전환을 정지한다.

## Accessibility

- 포커스 링은 3px 세이지 계열 외곽선과 2px 여백을 사용한다.
- 아이콘 전용 버튼은 접근 가능한 이름을 가진다.
- 색상만으로 선택, 오류, 품절 상태를 전달하지 않는다.
- 44px보다 작은 터치 대상은 인접 여백을 포함해 최소 터치 면적을 확보한다.
