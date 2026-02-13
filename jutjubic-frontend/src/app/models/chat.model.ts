export interface ChatMessage {
  videoId: number;
  username: string;
  userFirstName: string;
  userLastName: string;
  message: string;
  timestamp: string;
  type: 'CHAT' | 'JOIN' | 'LEAVE';
}

