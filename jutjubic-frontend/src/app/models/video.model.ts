export interface Video {
  id: number;
  title: string;
  description: string;
  videoUrl: string;
  thumbnailUrl: string;
  durationSeconds: number;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  userId: number;
  username: string;
  userFirstName: string;
  userLastName: string;
  createdAt: string;
  isLikedByCurrentUser?: boolean;
  latitude?: number;
  longitude?: number;
  // Premiere field - only scheduledAt is stored, status is calculated dynamically by backend
  premiereScheduledAt?: string;
  premiereStatus?: 'SCHEDULED' | 'LIVE' | 'ENDED';
}

export interface PremiereStatus {
  videoId: number;
  status: 'SCHEDULED' | 'LIVE' | 'ENDED';
  scheduledAt: string | null;
  startedAt: string | null;
  startOffset: number;  // seconds from start
  videoDuration: number | null;
  canSeek: boolean;
  viewerCount: number;
  secondsUntilStart: number;
}

export interface VideoPageResponse {
  videos: Video[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
  hasNext: boolean;
  hasPrevious: boolean;
}
