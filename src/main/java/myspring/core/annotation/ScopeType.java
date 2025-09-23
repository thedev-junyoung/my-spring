package myspring.core.annotation;

public enum ScopeType {
    SINGLETON, // 없으면 생성, 있으면 재사용
    PROTOTYPE // 매번 새로 생성
}
