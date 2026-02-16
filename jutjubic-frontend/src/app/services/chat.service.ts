import { Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, Observable } from 'rxjs';
import { ChatMessage } from '../models/chat.model';
import { environment } from '../env/environment';
import SockJS from 'sockjs-client';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private stompClient: Client | null = null;
  private messagesSubject = new BehaviorSubject<ChatMessage[]>([]);
  private connectedSubject = new BehaviorSubject<boolean>(false);

  public messages$: Observable<ChatMessage[]> = this.messagesSubject.asObservable();
  public connected$: Observable<boolean> = this.connectedSubject.asObservable();

  private currentVideoId: number | null = null;

  constructor() {}

  connect(videoId: number, username: string, firstName: string, lastName: string): void {
    if (this.stompClient && this.stompClient.connected && this.currentVideoId === videoId) {
      return; // Already connected to this video
    }

    // Disconnect from previous video if any
    if (this.stompClient && this.stompClient.connected) {
      this.disconnect();
    }

    this.currentVideoId = videoId;
    this.messagesSubject.next([]); // Clear previous messages

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
      console.log('Connected to WebSocket', frame.command);
      this.connectedSubject.next(true);

      // Subscribe to the video chat topic
      this.stompClient?.subscribe(`/topic/chat/${videoId}`, (message: IMessage) => {
        this.onMessageReceived(message);
      });

      // Send join message
      this.sendJoinMessage(username, firstName, lastName);
    };

    this.stompClient.onStompError = (frame: any) => {
      console.error('STOMP error:', frame);
      this.connectedSubject.next(false);
    };

    this.stompClient.activate();
  }

  disconnect(): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.deactivate();
      this.connectedSubject.next(false);
      this.currentVideoId = null;
      this.messagesSubject.next([]);
    }
  }

  sendMessage(message: string, username: string, firstName: string, lastName: string): void {
    if (this.stompClient && this.stompClient.connected && this.currentVideoId) {
      const chatMessage: ChatMessage = {
        videoId: this.currentVideoId,
        username,
        userFirstName: firstName,
        userLastName: lastName,
        message,
        timestamp: new Date().toISOString(),
        type: 'CHAT'
      };

      this.stompClient.publish({
        destination: `/app/chat/${this.currentVideoId}/send`,
        body: JSON.stringify(chatMessage)
      });
    }
  }

  private sendJoinMessage(username: string, firstName: string, lastName: string): void {
    if (this.stompClient && this.stompClient.connected && this.currentVideoId) {
      const joinMessage: ChatMessage = {
        videoId: this.currentVideoId,
        username,
        userFirstName: firstName,
        userLastName: lastName,
        message: '',
        timestamp: new Date().toISOString(),
        type: 'JOIN'
      };

      this.stompClient.publish({
        destination: `/app/chat/${this.currentVideoId}/join`,
        body: JSON.stringify(joinMessage)
      });
    }
  }

  sendLeaveMessage(username: string, firstName: string, lastName: string): void {
    if (this.stompClient && this.stompClient.connected && this.currentVideoId) {
      const leaveMessage: ChatMessage = {
        videoId: this.currentVideoId,
        username,
        userFirstName: firstName,
        userLastName: lastName,
        message: '',
        timestamp: new Date().toISOString(),
        type: 'LEAVE'
      };

      this.stompClient.publish({
        destination: `/app/chat/${this.currentVideoId}/leave`,
        body: JSON.stringify(leaveMessage)
      });
    }
  }

  private onMessageReceived(message: IMessage): void {
    const chatMessage: ChatMessage = JSON.parse(message.body);
    const currentMessages = this.messagesSubject.value;
    this.messagesSubject.next([...currentMessages, chatMessage]);
  }
}



