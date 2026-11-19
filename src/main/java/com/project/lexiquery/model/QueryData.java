package com.project.lexiquery.model;

import java.util.List;

public record QueryData(List<String> columns, List<List<Object>> rows) {}
