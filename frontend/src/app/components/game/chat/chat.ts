import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Message } from '../../../models/Message';

@Component({
  selector: 'app-game-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.html',
  styleUrl: './chat.css',
})
export class GameChatComponent {
  @Input() messages: Message[] = [];
  @Input() myUsername: string = '';

  @Output() messageSent = new EventEmitter<string>();

  newMessage = '';

  send() {
    const text = this.newMessage.trim();
    if (!text) return;
    this.messageSent.emit(text);
    this.newMessage = '';
  }
}
