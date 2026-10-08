package com.example.fairchance.util;

import java.util.List;

/**
 * Utility class for exporting entrant data to CSV format.
 */
public class CsvExporter {

    /**
     * Represents a single row in the exported CSV, containing basic entrant data.
     */
    public static class EntrantRow {
        private final String name;
        private final String email;
        private final String status;

        /**
         * Creates a new entrant row with the given fields.
         *
         * @param name   the entrant's name
         * @param email  the entrant's email address
         * @param status the entrant's status (e.g., "selected", "waitlisted", "not selected")
         */
        public EntrantRow(String name, String email, String status) {
            this.name = name;
            this.email = email;
            this.status = status;
        }

        /**
         * @return the entrant's name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the entrant's email address
         */
        public String getEmail() {
            return email;
        }

        /**
         * @return the entrant's status
         */
        public String getStatus() {
            return status;
        }
    }

    /**
     * Generates a CSV string with the header:
     * <pre>name,email,status</pre>
     * followed by one line per entrant.
     *
     * @param entrants list of {@link EntrantRow} to export
     * @return a CSV-formatted string containing all entrants
     */
    public static String generateCsv(List<EntrantRow> entrants) {
        StringBuilder sb = new StringBuilder();
        sb.append("name,email,status\n");

        for (EntrantRow row : entrants) {
            sb.append(row.getName())
                    .append(",")
                    .append(row.getEmail())
                    .append(",")
                    .append(row.getStatus())
                    .append("\n");
        }

        return sb.toString();
    }
}
