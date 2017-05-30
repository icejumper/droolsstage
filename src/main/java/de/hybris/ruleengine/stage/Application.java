package de.hybris.ruleengine.stage;

import de.hybris.ruleengine.stage.actions.OrderPercentageDiscountRAOAction;
import de.hybris.ruleengine.stage.utils.ModuleVersionUtils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public OrderPercentageDiscountRAOAction orderPercentageDiscountRAOAction()
    {
        return new OrderPercentageDiscountRAOAction();
    }

    @Bean
    public ModuleVersionUtils moduleVersionUtils()
    {
        return new ModuleVersionUtils();
    }
}
