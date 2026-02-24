package de.fi.tag2_06partitionierer;



import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcOperations;

import java.util.HashMap;
import java.util.Map;

public class ColumnRangePartitioner implements Partitioner {


        private final JdbcOperations jdbcTemplate;
        private final String table;
        private final String column;

        // Konstruktor für Injection
        public ColumnRangePartitioner(JdbcOperations jdbcTemplate, String table, String column) {
            this.jdbcTemplate = jdbcTemplate;
            this.table = table;
            this.column = column;
        }

        @Override
        public Map<String, ExecutionContext> partition(int gridSize) {
            // Dynamische Abfrage der aktuellen Range
            int min = jdbcTemplate.queryForObject("SELECT MIN(" + column + ") FROM " + table, Integer.class);
            int max = jdbcTemplate.queryForObject("SELECT MAX(" + column + ") FROM " + table, Integer.class);

            int targetSize = (max - min) / gridSize + 1;

        Map<String, ExecutionContext> result = new HashMap<>();
        int number = 0;
        int start = min;
        int end = start + targetSize - 1;

        while (start <= max) {
            ExecutionContext value = new ExecutionContext();
            result.put("partition" + number, value);
            if (end > max) end = max;
            value.putInt("minValue", start);
            value.putInt("maxValue", end);
            start += targetSize;
            end += targetSize;
            number++;
        }
        return result;
    }
}