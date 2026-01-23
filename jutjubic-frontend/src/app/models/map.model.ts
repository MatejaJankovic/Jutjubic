export interface MapBounds {
  north: number;
  south: number;
  east: number;
  west: number;
}

export interface VideoMarker {
  id: number;
  title: string;
  latitude: number;
  longitude: number;
  thumbnailUrl: string;
  viewCount: number;
  createdAt: string;
  username: string;
}

export interface VideoTile {
  zoom: number;
  tileX: number;
  tileY: number;
  videos: VideoMarker[];
  representativeVideo?: VideoMarker; // Za highest zoom levels
  videoCount: number;
}

export interface MapFilter {
  timePeriod: 'all' | 'last30days' | 'currentYear';
}

export type ZoomLevel = 'overview' | 'medium' | 'detailed';

export interface TileConfig {
  minZoom: number;
  maxZoom: number;
  maxVideosPerTile: number;
  level: ZoomLevel;
}

export const TILE_CONFIGS: Record<ZoomLevel, TileConfig> = {
  overview: {
    minZoom: 1,
    maxZoom: 5,
    maxVideosPerTile: 3,
    level: 'overview'
  },
  medium: {
    minZoom: 6,
    maxZoom: 10,
    maxVideosPerTile: 10,
    level: 'medium'
  },
  detailed: {
    minZoom: 11,
    maxZoom: 15,
    maxVideosPerTile: -1, // unlimited
    level: 'detailed'
  }
};

