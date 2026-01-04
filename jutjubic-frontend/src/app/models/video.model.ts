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
}

export interface VideoPageResponse {
  videos: Video[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
  hasNext: boolean;
  hasPrevious: boolean;
}
