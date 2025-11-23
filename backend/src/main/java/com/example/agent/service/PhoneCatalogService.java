package com.example.agent.service;

import com.example.agent.model.Phone;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PhoneCatalogService {

    private static final Logger logger = LoggerFactory.getLogger(PhoneCatalogService.class);

    private List<Phone> phones = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void loadPhones() {
        try {
            // Try to load from classpath first (for packaged JAR)
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("phones.json");
            if (inputStream == null) {
                // Fallback to file system (for development)
                ClassPathResource resource = new ClassPathResource("phones.json");
                inputStream = resource.getInputStream();
            }
            phones = objectMapper.readValue(inputStream, new TypeReference<List<Phone>>() {});
            logger.info("Successfully loaded {} phones from catalog", phones.size());
        } catch (IOException e) {
            logger.error("Error loading phones.json: {}", e.getMessage(), e);
            phones = new ArrayList<>();
        }
    }

    public List<Phone> getAllPhones() {
        return new ArrayList<>(phones);
    }

    public List<Phone> searchByBrand(String brand) {
        return phones.stream()
                .filter(phone -> phone.getBrand() != null && 
                        phone.getBrand().equalsIgnoreCase(brand))
                .collect(Collectors.toList());
    }

    public List<Phone> searchByPriceRange(Integer maxPrice) {
        return phones.stream()
                .filter(phone -> phone.getPrice() != null && phone.getPrice() <= maxPrice)
                .collect(Collectors.toList());
    }

    public List<Phone> searchByFeature(String feature) {
        return phones.stream()
                .filter(phone -> phone.getFeatures() != null &&
                        phone.getFeatures().stream()
                                .anyMatch(f -> f.toLowerCase().contains(feature.toLowerCase())))
                .collect(Collectors.toList());
    }

    public List<Phone> searchByName(String name) {
        return phones.stream()
                .filter(phone -> phone.getName() != null &&
                        phone.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    public Phone findByName(String name) {
        return phones.stream()
                .filter(phone -> phone.getName() != null &&
                        phone.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<Phone> findMultipleByName(List<String> names) {
        return phones.stream()
                .filter(phone -> phone.getName() != null &&
                        names.stream().anyMatch(name -> 
                                phone.getName().equalsIgnoreCase(name)))
                .collect(Collectors.toList());
    }
}

