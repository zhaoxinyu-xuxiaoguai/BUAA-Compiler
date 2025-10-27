package utils;

import java.util.Objects;
import java.util.PrimitiveIterator;

public class  Quadruple<Q1,Q2,Q3,Q4>{
    private Q1 first;
    private Q2 second;
    private Q3 third;
    private Q4 fourth;

    public Quadruple(Q1 first, Q2 second, Q3 third, Q4 fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    public Q1 getFirst() {
        return first;
    }

    public void setFirst(Q1 first) {
        this.first = first;
    }

    public Q2 getSecond() {
        return second;
    }

    public void setSecond(Q2 second) {
        this.second = second;
    }

    public Q3 getThird() {
        return third;
    }

    public void setThird(Q3 third) {
        this.third = third;
    }

    public Q4 getFourth() {
        return fourth;
    }

    public void setFourth(Q4 fourth) {
        this.fourth = fourth;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Quadruple<?, ?, ?, ?> quadruple = (Quadruple<?, ?, ?, ?>) o;
        return Objects.equals(first, quadruple.first) && Objects.equals(second, quadruple.second) && Objects.equals(third, quadruple.third) && Objects.equals(fourth, quadruple.fourth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, third, fourth);
    }
}
