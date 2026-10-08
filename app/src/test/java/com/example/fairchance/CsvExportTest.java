package com.example.fairchance;

import com.example.fairchance.util.CsvExporter.EntrantRow;
import com.example.fairchance.util.CsvExporter;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

public class CsvExportTest {

    @Test
    public void csvContainsHeaderAndAllRows() {
        List<EntrantRow> entrants = Arrays.asList(
                new EntrantRow("Alice", "alice@example.com", "enrolled"),
                new EntrantRow("Bob", "bob@example.com", "enrolled"),
                new EntrantRow("Charlie", "charlie@example.com", "cancelled")
        );

        String csv = CsvExporter.generateCsv(entrants);
        String[] lines = csv.split("\n");

        assertEquals(4, lines.length);
        assertEquals("name,email,status", lines[0]);
        assertEquals("Alice,alice@example.com,enrolled", lines[1]);
    }

    @Test
    public void csvEmptyList_stillHasHeaderOnly() {
        List<EntrantRow> entrants = Arrays.asList();
        String csv = CsvExporter.generateCsv(entrants);
        String[] lines = csv.split("\n");
        assertTrue(lines[0].equals("name,email,status"));
    }
}
