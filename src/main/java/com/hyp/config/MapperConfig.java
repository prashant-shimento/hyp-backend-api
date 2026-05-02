package com.hyp.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

    @Bean
    ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper
                .getConfiguration()
                .setSkipNullEnabled(true)
                .setAmbiguityIgnored(true)
                .setPropertyCondition(Conditions.isNotNull())
                .setMatchingStrategy(MatchingStrategies.STRICT);

        // Preserve insertion order when mapping Map -> Map
        modelMapper.addConverter(
                ctx -> ctx.getSource() == null
                        ? new LinkedHashMap<>()
                        : new LinkedHashMap<>((Map<?, ?>) ctx.getSource()),
                Map.class,
                LinkedHashMap.class);

        return modelMapper;
    }
}
