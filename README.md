# 스프링 데이터 JPA
김영한님의 "실전! 스프링 데이터 JPA 강의"를 보고 실습하는 repo

## 예제 도메인 모델

![domain model](https://ibb.co/Z2rrbxv)

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

    public void changeTeam(Team team){
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