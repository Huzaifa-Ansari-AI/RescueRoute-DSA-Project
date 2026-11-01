package com.huzaifa.rescueroute.common.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Executes safe, forward-only database migrations for Phase 3 on application startup.
 * Ensures the PostgreSQL schema is synchronized with entity mappings without requiring manual intervention.
 */
@Configuration
public class DatabaseMigrationInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationInitializer.class);

    private final DataSource dataSource;

    public DatabaseMigrationInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void applyMigrations() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            log.info("[RescueRoute] Executing Phase 3 forward-only database migrations...");

            // 1. verification_requests migrations
            stmt.execute("ALTER TABLE verification_requests ADD COLUMN IF NOT EXISTS proposed_changes TEXT;");
            stmt.execute("ALTER TABLE verification_requests ADD COLUMN IF NOT EXISTS request_type VARCHAR(50) NOT NULL DEFAULT 'CREATION';");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_verification_requests_request_type ON verification_requests(request_type);");

            // 2. facilities migrations
            stmt.execute("ALTER TABLE facilities ADD COLUMN IF NOT EXISTS has_pending_update BOOLEAN NOT NULL DEFAULT FALSE;");
            stmt.execute("ALTER TABLE facilities ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_facilities_has_pending_update ON facilities(has_pending_update);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_facilities_deleted_at ON facilities(deleted_at);");

            // 3. Phase 4: external_facilities table & indexes
            stmt.execute("CREATE TABLE IF NOT EXISTS external_facilities (" +
                    "id UUID PRIMARY KEY DEFAULT gen_random_uuid(), " +
                    "source VARCHAR(50) NOT NULL DEFAULT 'OPENSTREETMAP', " +
                    "osm_element_type VARCHAR(20) NOT NULL, " +
                    "osm_element_id BIGINT NOT NULL, " +
                    "source_url TEXT, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "facility_type VARCHAR(50) NOT NULL, " +
                    "osm_tags TEXT, " +
                    "latitude DOUBLE PRECISION NOT NULL, " +
                    "longitude DOUBLE PRECISION NOT NULL, " +
                    "address TEXT, " +
                    "city VARCHAR(100) DEFAULT 'Karachi', " +
                    "phone_number VARCHAR(100), " +
                    "website TEXT, " +
                    "opening_hours TEXT, " +
                    "import_status VARCHAR(50) NOT NULL DEFAULT 'DISCOVERED', " +
                    "matched_facility_id UUID REFERENCES facilities(id) ON DELETE SET NULL, " +
                    "imported_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, " +
                    "last_seen_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, " +
                    "created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT uq_external_facilities_source_element UNIQUE (source, osm_element_type, osm_element_id)" +
                    ");");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ext_facilities_source_osm ON external_facilities(source, osm_element_type, osm_element_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ext_facilities_status ON external_facilities(import_status);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ext_facilities_type ON external_facilities(facility_type);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ext_facilities_coords ON external_facilities(latitude, longitude);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ext_facilities_matched_facility ON external_facilities(matched_facility_id);");

            log.info("[RescueRoute] Phase 3 and Phase 4 database migrations completed successfully.");
        } catch (Exception e) {
            log.warn("[RescueRoute] Database migration note: {}", e.getMessage());
        }
    }
}
