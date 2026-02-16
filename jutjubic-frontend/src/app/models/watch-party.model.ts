export interface WatchParty {
  id: number;
  creatorId: number;
  videoId: number;
  currentVideoId: number;
  createdAt: string;
  isPublic: boolean;
  inviteCode: string;
  participantIds: number[];
}

export interface CreateWatchPartyRequest {
  videoId: number;
  isPublic: boolean;
}

export interface WatchPartyPlayEvent {
  roomId: number;
  videoId: number;
  triggeredBy: number;
  triggeredAt: string;
}

export interface WatchPartyEvent {
  type: 'USER_JOINED' | 'START' | 'VIDEO_SWITCHED' | 'PLAYBACK_PLAY' | 'PLAYBACK_PAUSE' | 'PLAYBACK_SEEK' | 'PLAYBACK_SYNC_REQUEST' | 'PLAYBACK_SYNC_RESPONSE';
  roomId: number;
  videoId: number;
  previousVideoId?: number;
  userId?: number;
  triggeredBy?: number;
  participantCount?: number;
  timestamp: string;
  // Playback sync fields
  currentTime?: number;
  isPlaying?: boolean;
}

export interface SwitchVideoRequest {
  videoId: number;
}

