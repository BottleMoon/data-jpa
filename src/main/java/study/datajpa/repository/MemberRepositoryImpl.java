package study.datajpa.repository;

import study.datajpa.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;

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
