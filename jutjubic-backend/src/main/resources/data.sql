-- Test korisnik (aktiviran, lozinka: Test@12345)
INSERT INTO users (email, username, password, first_name, last_name, address, enabled, role, created_at)
VALUES (
    'jankovicmatejabp@gmail.com',
    'mateja_jankovic',
    '$2b$10$u0hihEqTgbpXKdB3wUeJ9.dOFB.oU5YSeACPtft6zB.E1tFl9gh/2',
    'Mateja', 'Jankovic', 'Novi Sad',
    true, 'USER', NOW()
)
ON CONFLICT (email) DO NOTHING;

-- Test video (potreban za Watch Party kreiranje, video_id=1)
INSERT INTO videos (title, description, video_url, thumbnail_compressed, view_count, like_count, comment_count, user_id, created_at, updated_at)
SELECT
    'Test Video', 'Test video za Watch Party', 'https://example.com/test.mp4',
    false, 0, 0, 0,
    id, NOW(), NOW()
FROM users
WHERE email = 'jankovicmatejabp@gmail.com'
AND NOT EXISTS (
    SELECT 1 FROM videos WHERE video_url = 'https://example.com/test.mp4'
);
