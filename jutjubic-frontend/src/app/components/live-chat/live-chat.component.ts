import { Component, Input, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../services/chat.service';
import { ChatMessage } from '../../models/chat.model';
import { AuthService } from '../../services/auth.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-live-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './live-chat.component.html',
  styleUrls: ['./live-chat.component.scss']
})
export class LiveChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  @Input() videoId!: number;
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  messages: ChatMessage[] = [];
  newMessage = '';
  connected = false;

  private subscriptions = new Subscription();
  private shouldScrollToBottom = false;

  constructor(
    private chatService: ChatService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    if (!this.videoId) {
      console.error('VideoId is required for live chat');
      return;
    }

    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) {
      console.error('User must be authenticated to use chat');
      return;
    }

    // Subscribe to messages
    this.subscriptions.add(
      this.chatService.messages$.subscribe(messages => {
        this.messages = messages;
        this.shouldScrollToBottom = true;
      })
    );

    // Subscribe to connection status
    this.subscriptions.add(
      this.chatService.connected$.subscribe(connected => {
        this.connected = connected;
      })
    );

    // Connect to chat
    this.chatService.connect(
      this.videoId,
      currentUser.username,
      currentUser.firstName,
      currentUser.lastName
    );
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  ngOnDestroy(): void {
    const currentUser = this.authService.getCurrentUser();
    if (currentUser) {
      this.chatService.sendLeaveMessage(
        currentUser.username,
        currentUser.firstName,
        currentUser.lastName
      );
    }
    this.chatService.disconnect();
    this.subscriptions.unsubscribe();
  }

  sendMessage(): void {
    if (!this.newMessage.trim() || !this.connected) {
      return;
    }

    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) return;

    this.chatService.sendMessage(
      this.newMessage.trim(),
      currentUser.username,
      currentUser.firstName,
      currentUser.lastName
    );

    this.newMessage = '';
  }

  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private scrollToBottom(): void {
    try {
      this.messagesContainer.nativeElement.scrollTop =
        this.messagesContainer.nativeElement.scrollHeight;
    } catch (err) {
      console.error('Error scrolling to bottom:', err);
    }
  }

  getUserInitials(message: ChatMessage): string {
    return (message.userFirstName?.charAt(0) || '') + (message.userLastName?.charAt(0) || '');
  }

  getFormattedUsername(message: ChatMessage): string {
    return message.username;
  }

  formatTime(timestamp: string): string {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('sr-RS', { hour: '2-digit', minute: '2-digit' });
  }

  isSystemMessage(message: ChatMessage): boolean {
    return message.type === 'JOIN' || message.type === 'LEAVE';
  }

  getSystemMessageText(message: ChatMessage): string {
    if (message.type === 'JOIN') {
      return `${message.username} se pridružio/la chatu`;
    } else if (message.type === 'LEAVE') {
      return `${message.username} napustio/la chat`;
    }
    return '';
  }
}

