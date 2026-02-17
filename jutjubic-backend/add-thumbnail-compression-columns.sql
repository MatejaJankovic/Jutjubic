-- Migration: Add thumbnail compression fields to videos table
-- Date: 2026-02-16
-- Description: Adds fields to track thumbnail compression status

-- Add thumbnail_compressed column (default false, not null)
ALTER TABLE videos
ADD COLUMN IF NOT EXISTS thumbnail_compressed BOOLEAN NOT NULL DEFAULT false;

-- Add compressed_thumbnail_path column (nullable)
ALTER TABLE videos
ADD COLUMN IF NOT EXISTS compressed_thumbnail_path VARCHAR(255);

-- Add comment for documentation
COMMENT ON COLUMN videos.thumbnail_compressed IS 'Indicates if thumbnail has been compressed (ISA 3.9)';
COMMENT ON COLUMN videos.compressed_thumbnail_path IS 'Path to compressed thumbnail file (ISA 3.9)';

-- Verify the columns were added
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'videos'
AND column_name IN ('thumbnail_compressed', 'compressed_thumbnail_path')
ORDER BY column_name;

