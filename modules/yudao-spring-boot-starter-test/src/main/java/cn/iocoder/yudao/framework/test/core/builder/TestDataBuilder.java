package cn.iocoder.yudao.framework.test.core.builder;

import java.util.function.Consumer;

/**
 * Deterministic test data builder base.
 *
 * <p>Prefer readable defaults over random values. Use randomness only when a
 * field must be unique for the test.
 */
public abstract class TestDataBuilder<T, SELF extends TestDataBuilder<T, SELF>> {

    protected final T data;

    protected TestDataBuilder(T data) {
        this.data = data;
    }

    public SELF modify(Consumer<T> modifier) {
        modifier.accept(data);
        return self();
    }

    public T build() {
        return data;
    }

    @SuppressWarnings("unchecked")
    protected SELF self() {
        return (SELF) this;
    }

}
