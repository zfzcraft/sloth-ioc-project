package cn.zfzcraft.sloth.annotations;
import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalOnMissingBean {
}