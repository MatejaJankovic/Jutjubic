import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, Subject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../env/environment';
import { WatchParty, CreateWatchPartyRequest, WatchPartyEvent, SwitchVideoRequest } from '../models/watch-party.model';
import { VideoPageResponse } from '../models/video.model';

@Injectable({
  providedIn: 'root'
})
export class WatchPartyService {
  private readonly API_URL = `${environment.apiUrl}/api/watch-party`;
  private stompClient: Client | null = null;
  private connectedSubject = new BehaviorSubject<boolean>(false);
  private eventSubject = new Subject<WatchPartyEvent>();

  public connected$: Observable<boolean> = this.connectedSubject.asObservable();

  private currentRoomId: number | null = null;

  constructor(private http: HttpClient) {}

  /**
   * Create a new watch party room
   */
  createRoom(videoId: number, isPublic: boolean): Observable<WatchParty> {
    const request: CreateWatchPartyRequest = { videoId, isPublic };
    return this.http.post<WatchParty>(`${this.API_URL}/create`, request);
  }

  /**
   * Join an existing watch party room by invite code
   */
  joinRoom(inviteCode: string): Observable<WatchParty> {
    return this.http.post<WatchParty>(`${this.API_URL}/join/${inviteCode}`, {});
  }

  /**
   * Get watch party room details by ID
   */
  getRoom(roomId: number): Observable<WatchParty> {
    return this.http.get<WatchParty>(`${this.API_URL}/${roomId}`);
  }

  /**
   * Connect to WebSocket and return Observable of WatchPartyEvents
   */
  connect(roomId: number): Observable<WatchPartyEvent> {
    if (this.stompClient && this.stompClient.connected && this.currentRoomId === roomId) {
      return this.eventSubject.asObservable();
    }

    // Disconnect from previous room if any
    if (this.stompClient && this.stompClient.connected) {
      this.disconnect();
    }

    this.currentRoomId = roomId;

    const socket = new SockJS(environment.apiUrl + '/ws');
    this.stompClient = new Client({
      webSocketFactory: () => socket as any,
      debug: (str: string) => {
        console.log('STOMP: ' + str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.stompClient.onConnect = (frame: any) => {
      console.log('Connected to Watch Party WebSocket', frame.command);
      this.connectedSubject.next(true);

      // Subscribe to the watch party room topic
      this.stompClient?.subscribe(`/topic/watch-party/${roomId}`, (message: IMessage) => {
        this.onEventReceived(message);
      });

      console.log(`Subscribed to /topic/watch-party/${roomId}`);
    };

    this.stompClient.onStompError = (frame: any) => {
      console.error('STOMP error:', frame);
      this.connectedSubject.next(false);
    };

    this.stompClient.onDisconnect = () => {
      console.log('Disconnected from Watch Party WebSocket');
      this.connectedSubject.next(false);
    };

    this.stompClient.activate();

    return this.eventSubject.asObservable();
  }

  /**
   * Disconnect from WebSocket
   */
  disconnect(): void {
    if (this.stompClient) {
      if (this.stompClient.connected) {
        this.stompClient.deactivate();
      }
      this.connectedSubject.next(false);
      this.currentRoomId = null;
      this.stompClient = null;
    }
  }

  /**
   * Send START event to all users in the room
   */
  sendStart(roomId: number, videoId: number): void {
    if (this.stompClient && this.stompClient.connected) {
      const startEvent: WatchPartyEvent = {
        type: 'START',
        roomId,
        videoId,
        timestamp: new Date().toISOString()
      };

      this.stompClient.publish({
        destination: `/app/watch-party/${roomId}/start`,
        body: JSON.stringify(startEvent)
      });

      console.log('START event sent:', startEvent);
    } else {
      console.warn('Cannot send START event: WebSocket not connected');
    }
  }

  /**
   * Switch the current video in a watch party room via HTTP (owner only)
   */
  switchVideo(roomId: number, videoId: number): Observable<WatchParty> {
    const request: SwitchVideoRequest = { videoId };
    return this.http.post<WatchParty>(`${this.API_URL}/${roomId}/switch-video`, request);
  }

  /**
   * Send switch video event via WebSocket (owner only)
   */
  sendSwitchVideo(roomId: number, videoId: number, userId: number): void {
    if (this.stompClient && this.stompClient.connected) {
      const switchEvent = {
        roomId,
        videoId,
        userId,
        timestamp: new Date().toISOString()
      };

      this.stompClient.publish({
        destination: `/app/watch-party/${roomId}/switch-video`,
        body: JSON.stringify(switchEvent)
      });

      console.log('SWITCH_VIDEO event sent:', switchEvent);
    } else {
      console.warn('Cannot send SWITCH_VIDEO event: WebSocket not connected');
    }
  }

  /**
   * Get recommended videos for watch party (paginated, random order)
   */
  getRecommendedVideos(excludeVideoId?: number, page: number = 0, size: number = 10): Observable<VideoPageResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (excludeVideoId) {
      params = params.set('excludeVideoId', excludeVideoId.toString());
    }

    return this.http.get<VideoPageResponse>(`${this.API_URL}/recommended`, { params });
  }

  /**
   * Send PLAYBACK_PLAY event (owner plays video)
   */
  sendPlaybackPlay(roomId: number, currentTime: number, userId: number): void {
    if (this.stompClient && this.stompClient.connected) {
      const event = {
        roomId,
        currentTime,
        userId,
        timestamp: new Date().toISOString()
      };

      this.stompClient.publish({
        destination: `/app/watch-party/${roomId}/playback-play`,
        body: JSON.stringify(event)
      });

      console.log('PLAYBACK_PLAY event sent:', event);
    }
  }

  /**
   * Send PLAYBACK_PAUSE event (owner pauses video)
   */
  sendPlaybackPause(roomId: number, currentTime: number, userId: number): void {
    if (this.stompClient && this.stompClient.connected) {
      const event = {
        roomId,
        currentTime,
        userId,
        timestamp: new Date().toISOString()
      };

      this.stompClient.publish({
        destination: `/app/watch-party/${roomId}/playback-pause`,
        body: JSON.stringify(event)
      });

      console.log('PLAYBACK_PAUSE event sent:', event);
    }
  }

  /**
   * Send PLAYBACK_SEEK event (owner seeks to position)
   */
  sendPlaybackSeek(roomId: number, currentTime: number, userId: number): void {
    if (this.stompClient && this.stompClient.connected) {
      const event = {
        roomId,
        currentTime,
        userId,
        timestamp: new Date().toISOString()
      };

      this.stompClient.publish({
        destination: `/app/watch-party/${roomId}/playback-seek`,
        body: JSON.stringify(event)
      });

      console.log('PLAYBACK_SEEK event sent:', event);
    }
  }

  /**
   * Request playback sync from owner (guest joining)
   */
  sendPlaybackSyncRequest(roomId: number, userId: number): void {
    if (this.stompClient && this.stompClient.connected) {
      const event = {
        roomId,
        userId,
        timestamp: new Date().toISOString()
      };

      this.stompClient.publish({
        destination: `/app/watch-party/${roomId}/playback-sync-request`,
        body: JSON.stringify(event)
      });

      console.log('PLAYBACK_SYNC_REQUEST event sent:', event);
    }
  }

  /**
   * Send current playback state (owner responding to sync request)
   */
  sendPlaybackSyncResponse(roomId: number, currentTime: number, isPlaying: boolean, userId: number): void {
    if (this.stompClient && this.stompClient.connected) {
      const event = {
        roomId,
        currentTime,
        isPlaying,
        userId,
        timestamp: new Date().toISOString()
      };

      this.stompClient.publish({
        destination: `/app/watch-party/${roomId}/playback-sync-response`,
        body: JSON.stringify(event)
      });

      console.log('PLAYBACK_SYNC_RESPONSE event sent:', event);
    }
  }

  /**
   * Handle received events
   */
  private onEventReceived(message: IMessage): void {
    try {
      const event: WatchPartyEvent = JSON.parse(message.body);
      console.log('Watch Party event received:', event);
      this.eventSubject.next(event);
    } catch (e) {
      console.error('Error parsing watch party event:', e);
    }
  }

  /**
   * Check if WebSocket is connected
   */
  isConnected(): boolean {
    return this.stompClient?.connected ?? false;
  }
}

