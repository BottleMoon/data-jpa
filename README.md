# 스프링 데이터 JPA

김영한님의 "실전! 스프링 데이터 JPA 강의"를 보고 실습하는 repo. JPA기본을 선행학습 하고 스프링 데이터 JPA를 배우며
Spring에서 JPA를 더 편리하게 사용하는 법을 학습.

## 목차

- 예제 도메인 모델
    - 예제 도메인 모델과 동작확인
- 공통 인터페이스 기능
    - 순수 JPA 기반 리포지토리 만들기
    - 공통 인터페이스 설정
    - 공통 인터페이스 적용
    - 공통 인터페이스 분석
- 쿼리 메소드 기능
    - 메소드 이름으로 쿼리 생성
    - JPA NamedQuery
    - @Query, 리포지토리 메소드에 쿼리 정의하기
    - @Query, 값, DTO 조회하기
    - 파라미터 바인딩
    - 반환 타입
    - 순수 JPA 페이징과 정렬
    - 스프링 데이터 JPA 페이징과 정렬
    - 벌크성 수정 쿼리
    - @EntityGraph
    - JPA Hint & Lock
- 확장 기능
    - 사용자 정의 리포지토리 구현
    - Auditing
    - Web 확장 - 도메인 클래스 컨버터
    - Web 확장 - 페이징과 정렬
- 스프링 데이터 JPA 분석
    - 스프링 데이터 JPA 구현체 분석
    - 새로운 엔티티를 구별하는 방법

## 예제 도메인 모델

![domain model](https://i.ibb.co/Hhyy9qR/domain-model.png)

### Member 엔티티

Member.java

```java

@Entity
@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "username", "age"})
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}
```

### Team 엔티티

Team.java

```java

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Entity
public class Team {
    @Id
    @GeneratedValue
    @Column(name = "team_id")
    private Long id;
    private String name;

    @OneToMany(mappedBy = "team")
    private List<Member> members = new ArrayList<>();
}
```

### 데이터 확인 테스트

MemberTest.java

```
@SpringBootTest
@Transactional
@Rollback(value = false)
class MemberTest {

@PersistenceContext
EntityManagerem;

    @Test
    public void testEntity() {
        Team teamA = Team.builder().name("TeamA").build();
        Team teamB = Team.builder().name("TeamB").build();
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = Member.builder().username("member1").age(10).team(teamA).build();
        Member member2 = Member.builder().username("member2").age(10).team(teamA).build();
        Member member3 = Member.builder().username("member3").age(10).team(teamB).build();
        Member member4 = Member.builder().username("member4").age(10).team(teamB).build();
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();

List<Member> members = em.createQuery("select m from Member m", Member.class)
                .getResultList();

        for (Member member : members) {
            System.out.println("member = " + member);
            System.out.println("-> member.team = " + member.getTeam());

        }
    }
}
```

- 가급적 순수 JPA로 동작 확인 (뒤에서 변경)
- db 테이블 결과 확인
- 지연 로딩 동작 확인

## 공통 인터페이스 기능

- 순수 JPA 기반 리포지토리 만들기
- 스프링 데이터 JPA 공통 인터페이스 소개
- 스프링 데이터 JPA 공통 인터페이스 활용

### 순수 JPA 기반 리포지토리 만들기

- 기본 CRUD
    - 저장
    - 변경 변경감지 사용
    - 삭제
    - 전체 조회
    - 단건 조회
    - 카운트

**스프링 데이터 JPA를 구현체 없이 사용할 수 있는 이유**

![https://i.ibb.co/CbNMv9V/Screenshot-2022-11-01-at-2-29-34-PM.png](https://i.ibb.co/CbNMv9V/Screenshot-2022-11-01-at-2-29-34-PM.png)

- org.springframework.data.repository.Repository 를 구현한 클래스는 스캔 대상
    - MemberRepository 인터페이스가 동작한 이유
    - 실제 출력해보기(Proxy)
    - memberRepository.getClass() class com.sun.proxy.$ProxyXXX
- @Repository 애노테이션 생략 가능
    - 컴포넌트 스캔을 스프링 데이터 JPA가 자동으로 처리
    - JPA 예외를 스프링 예외로 변환하는 과정도 자동으로 처리

**스프링 데이터 JPA 기반 MemberRepository**

```java
public interface MemberRepository extends JpaRepository<Member, Long> {
}
```

**MemberRepository 테스트**

```java
class MemberRepositoryTest {
    @Autowired
    MemberRepository memberRepository;

    @Test
    public void testMember() {
        Member member = Member.builder().username("memberA").build();
        Member savedMember = memberRepository.save(member);

        Member findMember = memberRepository.findById(savedMember.getId()).get();

        assertThat(findMember.getId()).isEqualTo(member.getId());
        assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
        assertThat(findMember).isEqualTo(member);
    }

    @Test
    public void basicCRUD() {
        Member member1 = Member.builder().username("member1").build();
        Member member2 = Member.builder().username("member2").build();
        memberRepository.save(member1);
        memberRepository.save(member2);

        //단건 검증
        Member findMember1 = memberRepository.findById(member1.getId()).get();
        Member findMember2 = memberRepository.findById(member2.getId()).get();
        assertThat(findMember1).isEqualTo(member1);
        assertThat(findMember2).isEqualTo(member2);

        //리스트 조회 검증
        List<Member> all = memberRepository.findAll();
        assertThat(all.size()).isEqualTo(2);

        //카운트 검증
        long count = memberRepository.count();
        assertThat(count).isEqualTo(2);

        //삭제 검증
        memberRepository.delete(member1);
        memberRepository.delete(member2);
        count = memberRepository.count();
        assertThat(count).isEqualTo(0);
    }
}
```

**공통 인터페이스 분석**

- JpaRepository 인터페이스: 공통 CRUD 제공
- 제네릭은 <엔티티 타입, 식별자 타입> 설정

**공통 인터페이스 구성**

![https://i.ibb.co/TkHpFfh/Screenshot-2022-11-01-at-2-47-00-PM.png](https://i.ibb.co/TkHpFfh/Screenshot-2022-11-01-at-2-47-00-PM.png)

**주의**

- T findOne(ID) Optional<T> findById(ID) 변경

**제네릭 타입**

- T : 엔티티
- ID : 엔티티의 식별자 타입
- S : 엔티티와 그 자식 타입

**주요 메서드**

- save(S) : 새로운 엔티티는 저장하고 이미 있는 엔티티는 병합한다.
- delete(T) : 엔티티 하나를 삭제한다. 내부에서 EntityManager.remove() 호출
- findById(ID) : 엔티티 하나를 조회한다. 내부에서 EntityManager.find() 호출
- getOne(ID) : 엔티티를 프록시로 조회한다. 내부에서 EntityManager.getReference() 호출
- findAll(...) : 모든 엔티티를 조회한다. 정렬( Sort )이나 페이징( Pageable ) 조건을 파라미터로 제공할 수 있다.

> 참고: JpaRepository 는 대부분의 공통 메서드를 제공한다.

### 쿼리 메소드 기능

- 메소드 이름으로 쿼리 생성
- NamedQuery
- @Query - 리파지토리 메소드에 쿼리 정의
- 파라미터 바인딩
- 반환 타입
- 페이징과 정렬
- 벌크성 수정 쿼리
- @EntityGraph

**쿼리 메소드 기능 3가지**

- 메소드 이름으로 쿼리 생성
- 메소드 이름으로 JPA NamedQuery 호출
- @Query 어노테이션을 사용해서 레파지토리 인터페이스에 쿼리 직접 정의

메소드 이름으로 쿼리 생성

메소드 이름을 분석해서 JPQL 쿼리 실행

이름과 나이를 기준으로 회원을 조회하려면?

**순수 JPA 코드**

```java
public List<Member> findByUsernameAndAgeGreaterThan(String username,int age){
        return em.createQuery("select m from Member m where m.username = :username and m.age > :age",Member.class)
        .setParameter("username",username)
        .setParameter("age",age)
        .getResultList();
        }
```

**스프링 데이터 JPA 코드**

```java
List<Member> findByUsernameAndAgeGreaterThan(String username,int age);
```

- 스프링 데이터 JPA는 메소드 이름을 분석해서 JPQL을 생성하고 실행.

**쿼리 메소드 필터 조건**

스프링 데이터 JPA 공식 문서
참고: ([https://docs.spring.io/spring-data/jpa/docs/current/](https://docs.spring.io/spring-data/jpa/docs/current/)
reference/html/#jpa.query-methods.query-creation)

**스프링 데이터 JPA가 제공하는 쿼리 메소드 기능**

- 조회: find...By ,read...By ,query...By get...By,
    - https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation
    - 예:) findHelloBy 처럼 ...에 식별하기 위한 내용(설명)이 들어가도 된다.
- COUNT: count...By 반환타입 long
- EXISTS: exists...By 반환타입 boolean
- 삭제: delete...By, remove...By 반환타입 long
- DISTINCT: findDistinct, findMemberDistinctBy
- LIMIT: findFirst3, findFirst, findTop, findTop3
    - https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.limit-query-result

> 참고: 이 기능은 엔티티의 필드명이 변경되면 인터페이스에 정의한 메서드 이름도 꼭 함께 변경해야 한다. 그렇지 않으면 애플리케이션을 시작하는 시점에 오류가 발생한다.

> 이렇게 애플리케이션 로딩 시점에 오류를 인지할 수 있는 것이 스프링 데이터 JPA의 매우 큰 장점이다.

### @Query, 레포지토리 메소드에 쿼리 정의하기

메소드에 JPQL 쿼리 작성

```java
@Query("select m from Member m where m.username = :username and m.age = :age")
List<Member> findUser(@Param("username") String username,@Param("age") int age);
```

- @org.springframework.data.jpa.repository.Query 어노테이션을 사용
- 실행할 메서드에 정적 쿼리를 직접 작성하므로 이름 없는 Named 쿼리라 할 수 있음
- JPA Named 쿼리처럼 애플리케이션 실행 시점에 문법 오류를 발견할 수 있음(매우 큰 장점!)

> 참고: 실무에서는 메소드 이름으로 쿼리 생성 기능은 파라미터가 증가하면 메서드 이름이 매우
> 지저분해진다. 따라서 @Query 기능을 자주 사용하게 된다.

### @Query, 값, DTO 조회하기

**값 하나를 조회**

```java
@Query("select m.username from Member m")
List<String> findUserNameList();
```

**DTO로 직접 조회**

```java
@Query("select new study.datajpa.dto.MemberDto(m.id,m.username,t.name) from Member m join m.team t")
List<MemberDto> findMemberDto();
```

> 주의! DTO로 직접 조회 하려면 JPA의 new 명령어를 사용해야 한다. 그리고 다음과 같이 생성자가 맞는 DTO가 필요하다. (JPA와 사용방식이 동일하다.)

> 나중에 배울 QueryDSL을 쓰면 굉장히 편해진다.

### 파라미터 바인딩

- 이름 기반
- 위치 기반
    - 순서가 바뀌거나 실수를 할 수 있으니 이름 기반을 사용하자

```java
select m from Member m where m.username=?0 //위치 기반 
        select m from Member m where m.username=:name //이름 기반
```

**파라미터 바인딩**

```java
@Query("select m from Member m where m.username = :name")
Member findMembers(@Param("name") String username);
```

**컬렉션 파라미터 바인딩**

- **collection** 타입으로 in절 지원

```java
@Query("select m from Member m where m.username in :names")
List<Member> findByNames(@Param("names") List<String> names);
```

### 반환 타입

스프링 데이터 JPA 는 유연한 반환 타입을 지원

```java
List<Member> findByUsername(String name); //컬렉션 Member findByUsername(String name); //단건

        Optional<Member> findByUsername(String name); //단건 Optional
```

스프링 데이터 JPA 공식
문서: [https://docs.spring.io/spring-data/jpa/docs/current/reference/](https://docs.spring.io/spring-data/jpa/docs/current/reference/)
html/#repository-query-return-types

**조회 결과가 많거나 없으면?**

- 컬렉션
    - 결과 없음: 빈 컬렉션 반환(null이 아님!!)
- 단건 조회
    - 결과 없음: null 반환
    - 결과가 2건 이상: javax.persistence.NonUniqueResultException 예외 발생

### 스프링 데이터 JPA 페이징과 정렬

페이징과 정렬 파라미터

- org.springframework.data.domain.Sort : 정렬 기능
- org.springframework.data.domain.Pageable : 페이징 기능 (내부에 Sort 포함)

**특별한 반환 타입**

- org.springframework.data.domain.Page : 추가 count 쿼리 결과를 포함하는 페이징
- org.springframework.data.domain.Slice : 추가 count 쿼리 없이 다음 페이지만 확인 가능 (내부적으로 limit + 1조회)
- List (자바 컬렉션): 추가 count 쿼리 없이 결과만 반환

**페이지 정렬 사용 예제**

```java
Page<Member> findByUsername(String name,Pageable pageable); //count 쿼리 사용 
        Slice<Member> findByUsername(String name,Pageable pageable); //count 쿼리 사용 안함
        List<Member> findByUsername(String name,Pageable pageable); //count 쿼리 사용 안함
        List<Member> findByUsername(String name,Sort sort);
```

다음 조건으로 페이징과 정렬을 사용하는 예제 코드를 보자.

검색 조건: 나이가 10살
정렬 조건: 이름으로 내림차순
페이징 조건: 첫 번째 페이지, 페이지당 보여줄 데이터는 3건

**Page 사용 예제 정의 코드**

MemberRepository.java

```java
//Page<Member> findByAge(int age, Pageable pageable);
Slice<Member> findByAge(int age,Pageable pageable);
```

**Page 사용 예제 실행 코드**

```java
@Test
public void paging(){
        memberRepository.save(Member.builder().username("member1").age(10).build());
        memberRepository.save(Member.builder().username("member2").age(10).build());
        memberRepository.save(Member.builder().username("member3").age(10).build());
        memberRepository.save(Member.builder().username("member4").age(10).build());
        memberRepository.save(Member.builder().username("member5").age(10).build());

        int age=10;
        PageRequest pageRequest=PageRequest.of(0,3,Sort.by(Sort.Direction.DESC,"username"));

        // when
        //Page<Member> page = memberRepository.findByAge(age, pageRequest);
        Slice<Member> page=memberRepository.findByAge(age,pageRequest);

        //then
        List<Member> content=page.getContent();

        assertThat(content.size()).isEqualTo(3);
        //assertThat(page.getTotalElements()).isEqualTo(5);// total count
        assertThat(page.getNumber()).isEqualTo(0);// 페이지 번호
        //assertThat(page.getTotalPages()).isEqualTo(2);// 총 페이지 수
        assertThat(page.isFirst()).isTrue();// 첫번째 페이지인지
        assertThat(page.hasNext()).isTrue();// 다음 페이지 있는지
        }
```

- 두 번째 파라미터로 받은 Pagable 은 인터페이스다. 따라서 실제 사용할 때는 해당 인터페이스를 구현한
  org.springframework.data.domain.PageRequest 객체를 사용한다.
- PageRequest 생성자의 첫 번째 파라미터에는 현재 페이지를, 두 번째 파라미터에는 조회할 데이터 수를
  입력한다. 여기에 추가로 정렬 정보도 파라미터로 사용할 수 있다. 참고로 페이지는 0부터 시작한다.

> 주의: Page는 1부터 시작이 아니라 0부터 시작이다.

> Page는 Slice를 상속한다.

**참고: count 쿼리를 다음과 같이 분리할 수 있음**

```
@Query(value = “select m from Member m”, 
					countQuery = “select count(m.username) from Member m”)
Page<Member> findMemberAllCountBy(Pageable pageable);
```

- 전체 count는 매우 무겁고 query에 join이 들어가면 Page를 쓸 때 자동으로 날려주는 count query가 최적화가 잘 안되기 때문에 직접 count쿼리를 짜는 방법이 있다.

**페이지를 유지하면서 엔티티를 DTO로 변환하기**

```
Page<Member> page = memberRepository.findByAge(10, pageRequest);    
Page<MemberDto> dtoPage = page.map(m -> new MemberDto());
```

**실습**

- Page
- Slice (count X) 추가로 limit + 1을 조회한다. 그래서 다음 페이지 여부 확인(최근 모바일 리스트 생각해보면 됨)
- List (count X)
- 카운트 쿼리 분리(이건 복잡한 sql에서 사용, 데이터는 left join, 카운트는 left join 안해도 됨)
    - 실무에서 매우 중요!!!

### 벌크성 수정 쿼리

**JPA**

```java
public int bulkAgePlus(int age){
        return em.createQuery("update Member m set m.age = m.age + 1"+
        " where m.age >= :age"
        ).setParameter("age",age)
        .executeUpdate();
        }
```

**스프링 데이터 JPA**

```java
@Modifying //insert, update, delete같은 벌크성 쿼리에 써줘야함.
@Query("update Member m set m.age = m.age + 1 where m.age >= :age")
int bulkAgePlus(@Param("age") int age);
```

- 벌크성 수정, 삭제 쿼리는 @Modifying 어노테이션을 사용
    - 사용하지 않으면 다음 예외 발생
    - `org.hibernate.hql.internal.QueryExecutionRequestException: Not supported for`

  `DML operations`

- 벌크성 쿼리를 실행하고 나서 영속성 컨텍스트 초기화: @Modifying(clearAutomatically = true)
  (이 옵션의 기본값은 false )
    - 이 옵션 없이 회원을 findById로 다시 조회하면 영속성 컨텍스트에 과거 값이 남아서 문제가 될 수
      있다. 만약 다시 조회해야 하면 꼭 영속성 컨텍스트를 초기화 하자.

> 참고: 벌크 연산은 영속성 컨텍스트를 무시하고 실행하기 때문에, 영속성 컨텍스트에 있는 엔티티의 상태와
> DB에 엔티티 상태가 달라질 수 있다.

> 권장하는 방안
> 1. 영속성 컨텍스트에 엔티티가 없는 상태에서 벌크 연산을 먼저 실행한다.
> 2. 부득이하게 영속성 컨텍스트에 엔티티가 있으면 벌크 연산 직후 영속성 컨텍스트를 초기화 한다.

### @EntityGraph

```java
//공통 메서드 오버라이드
@Override
@EntityGraph(attributePaths = {"team"})
List<Member> findAll();

//JPQL + 엔티티 그래프
@EntityGraph(attributePaths = {"team"})
@Query("select m from Member m")
List<Member> findMemberEntityGraph();

//메서드 이름으로 쿼리에서 특히 편리하다.
@EntityGraph(attributePaths = {"team"})
List<Member> findByUsername(String username)
```

**EntityGraph 정리**

- 사실상 페치 조인(FETCH JOIN)의 간편 버전
- LEFT OUTER JOIN 사용

### JPA Hint & Lock

**JPA Hint**

JPA 쿼리 힌트(SQL 힌트가 아니라 JPA 구현체에게 제공하는 힌트)

**쿼리 힌트 사용**

```java
@QueryHints(value = @QueryHint(name = "org.hibernate.readOnly", value = "true"))
Member findReadOnlyByUsername(String username);
```

**쿼리 힌트 사용 확인**

```java
public void queryHint(){
        //given
        Member member1=Member.builder().username("member1").age(10).build();
        memberRepository.save(member1);

        em.flush();
        em.clear();

        //변경 감지 작동
        //Member findMember = memberRepository.findById(member1.getId()).get();
        //findMember.changeUsername("member2");

        //QueryHint로 readOnly 로 설정해서 변경감지가 작동하지 않음.
        Member findMember=memberRepository.findReadOnlyByUsername("member1");
        findMember.changeUsername("member2");

        em.flush();

        }
```

## 확장 기능

### 사용자 정의 리포지토리 구현

- 스프링 데이터 JPA 리포지토리는 인터페이스만 정의하고 구현체는 스프링이 자동 생성
- 스프링 데이터 JPA가 제공하는 인터페이스를 직접 구현하면 구현해야 하는 기능이 너무 많음\
- 다양한 이유로 인터페이스의 메서드를 직접 구현하고 싶다면?
    - JPA 직접 사용( EntityManager )
    - 스프링 JDBC Template 사용
    - MyBatis 사용
    - 데이터베이스 커넥션 직접 사용 등등...
    - Querydsl 사용

**사용자 정의 인터페이스**

```java
public interface MemberRepositoryCustom {
    List<Member> findMemberCustom();
}
```

**사용자 정의 인터페이스 구현 클래스**

```java
//본 Repository의 클래스 이름 뒤에 Impl을 붙여야 JPA에서 인식할 수 있다.!!
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final EntityManager em;

    public MemberRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<Member> findMemberCustom() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }
}
```

**사용자 정의 인터페이스 상속**

```java
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
}
```

**사용자 정의 메서드 호출 코드**

```java
List<Member> result=memberRepository.findMemberCustom();
```

**사용자 정의 구현 클래스**

- 명명 규칙 (두번째 방법 추천)
    1. 리포지토리 인터페이스 이름 + Impl (MemberRepositoryImpl)
    2. 커스텀 리포지토리 인터페이스 이름 + Impl (MemberRepositoryCustomImpl)
- 스프링 데이터 JPA가 인식해서 스프링 빈으로 등록

**Impl 대신 다른 이름으로 변경하고 싶으면?
XML 설정**

```
  <repositories base-package="study.datajpa.repository"
                repository-impl-postfix="Impl" />

```

**JavaConfig 설정**

```
@EnableJpaRepositories(basePackages = "study.datajpa.repository",
                   repositoryImplementationPostfix = "Impl")

```

> 참고: 실무에서는 주로 QueryDSL이나 SpringJdbcTemplate을 함께 사용할 때 사용자 정의
> 리포지토리 기능 자주 사용

> 참고: 항상 사용자 정의 리포지토리가 필요한 것은 아니다. 그냥 임의의 리포지토리를 만들어도 된다.
> 예를들어 MemberQueryRepository를 인터페이스가 아닌 클래스로 만들고 스프링 빈으로 등록해서
> 그냥 직접 사용해도 된다. 물론 이 경우 스프링 데이터 JPA와는 아무런 관계 없이 별도로 동작한다.

### Auditing

- 엔티티를 생성, 변경할 때 변경한 사람과 신간을 추적하고 싶으면 ?
    - 등록일
    - 수정일
    - 등록자
    - 수정자

**순수 JPA 적용**

**우선 등록일, 수정일 적용**

```java

@Getter
@Setter
@MappedSuperclass //속성만 상속 (필수 추가)
public class JpaBaseEntity {

    @Column(updatable = false)
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    @PrePersist
    public void prePersist() {
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
```

```java
public class Member extends JpaBaseEntity {
}
```

**확인 코드**

```java
@Test
public void JpaEventBaseEntity()throws Exception{
        //given
        Member member=Member.builder().username("member1").build();
        memberRepository.save(member); //@PrePersist

        Thread.sleep(100);
        member.changeUsername("member2");

        em.flush();//@PreUpdate
        em.clear();

        //when
        Member findMember=memberRepository.findById(member.getId()).get();

        //then
        System.out.println("findMember = "+findMember.getCreatedDate());
        System.out.println("findMember = "+findMember.getUpdatedDate());
        }
```

**JPA 주요 이벤트 어노테이션**

- @PrePersist, @PostPersist
- @PreUpdate, @PostUpdate

**스프링 데이터 JPA 사용**

**설정**

@EnableJpaAuditing → 스프링 부트 설정 클래스에 적용해야함.

@EntityListeners(AuditingEntityListener.class) → 엔티티에 적용

**사용 어노테이션**

- @CreateDate
- @LastModifiedDate
- @CreateBy
- @LastModifiedBy

**스프링 데이터 Auditing 적용 - 등록일, 수정일**

```java

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}
```

**스프링 데이터 Auditing 적용 - 등록자, 수정자**

```java

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String lastModifiedBy;

}
```

**등록자, 수정자를 처리해주는 AuditorAware 스프링 빈 등록**

```java

@EnableJpaAuditing
@SpringBootApplication
public class DataJpaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataJpaApplication.class, args);
    }

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(UUID.randomUUID().toString());
    }

}
```

> 참고: 저장시점에 등록일, 등록자는 물론이고, 수정일, 수정자도 같은 데이터가 저장된다. 데이터가 중복
> 저장되는 것 같지만, 이렇게 해두면 변경 컬럼만 확인해도 마지막에 업데이트한 유저를 확인 할 수 있으므로
> 유지보수 관점에서 편리하다. 이렇게 하지 않으면 변경 컬럼이 null 일때 등록 컬럼을 또 찾아야 한다.

> 참고로 저장시점에 저장데이터만 입력하고 싶으면 @EnableJpaAuditing(modifyOnCreate = false)
> 옵션을 사용하면 된다.

### Web 확장 - 도메인 클래스 컨버터

HTTP 파라미터로 넘어온 엔티티의 아이디로 엔티티 객체를 찾아서 바인딩

**도메인 클래스 컨버터 사용 전**

```java
@GetMapping("/member/{id}")
public String findMember(@PathVariable("id") Long id){
        Member member=memberRepository.findById(id).get();
        return member.getUsername();
        }
```

**도메인 클래스 컨버터 사용 후**

```java
//도메인 클래스 컨버터
@GetMapping("/member2/{id}")
public String findMember2(@PathVariable("id") Member member){
        return member.getUsername();
        }
```

- HTTP 요청은 회원 id를 받지만 도메인 클래스 컨버터가 중간에 동작해서 회원 엔티티 객체를 반환
- 도메인 클래스 컨버터도 리파지토리를 사용해서 엔티티를 찾음

> **주의:** 도메인 클래스 컨버터로 엔티티를 파라미터로 받으면, 이 엔티티는 단순 조회용으로만 사용해야 한다.
(트랜잭션이 없는 범위에서 엔티티를 조회했으므로, 엔티티를 변경해도 DB에 반영되지 않는다.)

### Web 확장 - 페이징과 정렬

스프링 데이터가 제공하는 페이징과 정렬 기능을 스프링 MVC에서 편리하게 사용할 수 있다.

**페이징과 정렬 예제**

```java
@GetMapping("/members")
public Page<MemberDto> list(Pageable pageable){
        return memberRepository.findAll(pageable).map(MemberDto::new);
        }
```

- 파라미터로 Pageable을 받을 수 있다.
- Pageable은 인터페이스, 실제는 org.springframework.data.domain.PageRequest 객체 생성

**요청 파라미터**

- 예) /members?page=0&size&sort=id,desc&sort=username,desc
- page: 현재 페이지, 0부터 시작한다.
- size: 한 페이지에 노출할 데이터 건수
- sort: 정렬 조건을 정의한다. 예) 정렬 속성, 정력 속성…(ASC | DESC), 정렬 방향을 변경하고 싶으면 sort 파라미터 추가 ( asc 생략 가능)

**기본값**

- 글로벌 설정: 스프링 부트

```java
spring.data.web.pageable.default-page-size=20/# 기본 페이지 사이즈/
        spring.data.web.pageable.max-page-size=2000/# 최대 페이지 사이즈/
```

- 개별 설정

**@PageableDefault 어노테이션 사용**

```java
@GetMapping("/members")
public Page<MemberDto> list(@PageableDefault(size = 5, sort = "username",
        direction = Sort.Direction.DESC) Pageable pageable){
        ...
        }
```

**접두사**

- 페이징 정보가 둘 이상이면 접두사로 구분
- @Qualifier에 접두사명 추가 “(접두사명)_xxx”
- 예제:/members?member_page=0&order_page=1

```java
public String list(
@Qualifier("member") Pageable memberPageable,
@Qualifier("order") Pageable orderpagealbe,
        ...
        )
```

**Page를 1부터 시작하기**

- 스프링 데이터는 Page를 0부터 시작한다. 1부터 시작하려면 아래 방법을 사용하자.
    - Pageable, Page를 파리미터와 응답 값으로 사용히지 않고, 직접 클래스를 만들어서 처리한다. 그리고 직접 PageRequest(Pageable 구현체)를 생성해서 리포지토리에 넘긴다. 물론
      응답값도 Page 대신에 직접 만들어서 제공해야 한다.

### 새로운 엔티티를 구별하는 방법

**매우 중요!!!**

- *save() 메서드*
    - 새로운 엔티티면 저장( persist )
    - 새로운 엔티티가 아니면 병합( merge )
- 새로운 엔티티를 판단하는 기본 전략
    - 식별자가 객체일 때 null 로 판단
    - 식별자가 자바 기본 타입일 때 0 으로 판단
    - Persistable 인터페이스를 구현해서 판단 로직 변경 가능

> 참고: JPA 식별자 생성 전략이 @GenerateValue 면 save() 호출 시점에 식별자가 없으므로 새로운
> 엔티티로 인식해서 정상 동작한다. 그런데 JPA 식별자 생성 전략이 @Id 만 사용해서 직접 할당이면 이미
> 식별자 값이 있는 상태로 save() 를 호출한다. 따라서 이 경우 merge() 가 호출된다. merge() 는 우선
> DB를 호출해서 값을 확인하고, DB에 값이 없으면 새로운 엔티티로 인지하므로 매우 비효율 적이다. 따라서
> Persistable 를 사용해서 새로운 엔티티 확인 여부를 직접 구현하게는 효과적이다.

> 참고로 등록시간( @CreatedDate )을 조합해서 사용하면 이 필드로 새로운 엔티티 여부를 편리하게 확인할 수 있다. (@CreatedDate에 값이 없으면 새로운 엔티티로 판단)