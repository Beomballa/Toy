const ContentBoardConfig = (() => {
    // 게시판별 문구와 이동 경로를 한 곳에서 관리해 목록/상세/편집 화면을 같은 기준으로 맞춘다.
    const BOARD_META = {
        NOTICE: {
            badge: 'NOTICE',
            list: {
                pageTitle: '콘텐츠 관리',
                breadcrumb: '콘텐츠 관리',
                createLabel: '새 공지 작성',
                description: '운영 공지와 서비스 안내 문서를 관리합니다.',
                boardLabel: '공지'
            },
            edit: {
                title: '공지 작성',
                saveLabel: '공지 저장',
                boardName: '공지',
                description: '운영 공지와 서비스 안내를 명확하게 전달하는 문서를 작성합니다.',
                sideNote: '공지 게시판은 모든 운영자와 사용자가 가장 먼저 확인하는 정보성 영역입니다.'
            },
            detail: {
                pageTitle: '공지 상세',
                listTitle: '콘텐츠 관리',
                label: '공지사항'
            },
            listPath: '/admin/content/list?boardType=NOTICE'
        },
        STYLE: {
            badge: 'STYLE',
            list: {
                pageTitle: '스타일 피드',
                breadcrumb: '스타일 피드',
                createLabel: '새 스타일 피드 작성',
                description: '룩북, 착용 이미지, 큐레이션 피드를 관리합니다.',
                boardLabel: '스타일'
            },
            edit: {
                title: '스타일 피드 작성',
                saveLabel: '피드 저장',
                boardName: '스타일 피드',
                description: '룩북, 착용 이미지, 큐레이션 성격의 콘텐츠를 피드 형식으로 정리합니다.',
                sideNote: '스타일 피드는 시각적인 흐름이 중요하므로 제목과 첫 문장의 완성도가 특히 중요합니다.'
            },
            detail: {
                pageTitle: '스타일 피드 상세',
                listTitle: '스타일 피드',
                label: '스타일 피드'
            },
            listPath: '/admin/content/list?boardType=STYLE'
        },
        DISCUSS: {
            badge: 'DISCUSS',
            list: {
                pageTitle: '종목 토론방',
                breadcrumb: '종목 토론방',
                createLabel: '새 토론 작성',
                description: '상품별 이슈와 시세 흐름을 다루는 토론 게시글을 관리합니다.',
                boardLabel: '토론'
            },
            edit: {
                title: '종목 토론 작성',
                saveLabel: '토론 저장',
                boardName: '종목 토론방',
                description: '상품 이슈, 시세 흐름, 관심 포인트를 토론형 문맥에 맞춰 작성합니다.',
                sideNote: '토론형 게시판은 질문형 제목이나 핵심 이슈가 먼저 드러나는 문장이 더 잘 읽힙니다.'
            },
            detail: {
                pageTitle: '종목 토론 상세',
                listTitle: '종목 토론방',
                label: '종목 토론방'
            },
            listPath: '/admin/content/list?boardType=DISCUSS'
        },
        QNA: {
            badge: 'QNA',
            list: {
                pageTitle: '문의사항',
                breadcrumb: '문의사항',
                createLabel: '새 문의 작성',
                description: '사용자 문의와 응답이 필요한 게시글을 관리합니다.',
                boardLabel: '문의'
            },
            edit: {
                title: '문의 작성',
                saveLabel: '문의 저장',
                boardName: '문의사항',
                description: '사용자 문의와 답변 관리에 적합한 형태로 내용을 정리합니다.',
                sideNote: '문의 게시판은 요약 제목과 본문 내 맥락 분리가 잘 되어야 후속 대응이 쉽습니다.'
            },
            detail: {
                pageTitle: '문의 상세',
                listTitle: '문의사항',
                label: '문의사항'
            },
            listPath: '/admin/content/list?boardType=QNA'
        }
    };

    function normalizeBoardType(boardType) {
        return BOARD_META[boardType] ? boardType : 'NOTICE';
    }

    function getMeta(boardType) {
        return BOARD_META[normalizeBoardType(boardType)];
    }

    function getListPath(boardType) {
        return getMeta(boardType).listPath;
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    return {
        BOARD_META,
        normalizeBoardType,
        getMeta,
        getListPath,
        escapeHtml
    };
})();
