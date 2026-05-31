import { Component, signal, computed, ViewChild, ElementRef, AfterViewChecked, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AiService, ChatMessage, ChatResponse } from '../../core/services/ai.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-ai-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="chat-page">
      <div class="chat-header">
        <div class="header-left">
          <span class="bot-icon">🤖</span>
          <div>
            <h2>BookIt AI Assistant</h2>
            <p class="subtitle">Powered by GPT-4o-mini · Search, Book, Explore</p>
          </div>
        </div>
        <div class="header-actions">
          <button class="clear-btn" (click)="clearConversation()" title="New conversation">🗑 Clear</button>
          <a class="back-btn" routerLink="/search">← Search</a>
        </div>
      </div>

      <div class="messages-container" #messagesContainer>
        <!-- Welcome message -->
        <div class="welcome" *ngIf="messages().length === 0">
          <div class="welcome-icon">🤖</div>
          <h3>Hi! I'm BookIt AI</h3>
          <p>I can help you find flights, hotels, and cinema seats — and answer your booking questions.</p>
          <div class="quick-prompts">
            <button *ngFor="let prompt of quickPrompts" (click)="sendQuickPrompt(prompt)">{{ prompt }}</button>
          </div>
        </div>

        <!-- Messages -->
        <div class="message-list">
          <div *ngFor="let msg of messages()"
               class="message"
               [class.user-message]="msg.role === 'user'"
               [class.assistant-message]="msg.role === 'assistant'">
            <div class="message-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
            <div class="message-body">
              <div class="message-content" [innerHTML]="formatMessage(msg.content)"></div>
              <div class="message-time">{{ msg.timestamp | date:'HH:mm' }}</div>
            </div>
          </div>

          <!-- Typing indicator -->
          <div class="message assistant-message" *ngIf="loading()">
            <div class="message-avatar">🤖</div>
            <div class="message-body">
              <div class="typing-indicator">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Suggested actions -->
      <div class="suggestions" *ngIf="suggestions().length > 0 && !loading()">
        <button *ngFor="let action of suggestions()"
                class="suggestion-chip"
                (click)="sendQuickPrompt(action)">
          {{ action }}
        </button>
      </div>

      <!-- Input area -->
      <div class="input-area">
        <textarea
          [(ngModel)]="inputMessage"
          placeholder="Ask me anything — 'Find flights to London', 'What can I book?'..."
          (keydown.enter)="onEnterKey($event)"
          [disabled]="loading()"
          rows="1"
          class="message-input"
          #inputRef></textarea>
        <button class="send-btn"
                (click)="sendMessage()"
                [disabled]="loading() || !inputMessage.trim()">
          {{ loading() ? '⏳' : '➤' }}
        </button>
      </div>
      <div class="input-hint">Press Enter to send · Shift+Enter for new line</div>
    </div>
  `,
  styles: [`
    .chat-page { display:flex; flex-direction:column; height:calc(100vh - 56px); max-width:900px; margin:0 auto; background:#fff; }

    /* Header */
    .chat-header { display:flex; justify-content:space-between; align-items:center; padding:1rem 1.5rem;
                   border-bottom:1px solid #e0e0e0; background:#1976d2; color:white; }
    .header-left { display:flex; align-items:center; gap:.75rem; }
    .bot-icon { font-size:2rem; }
    .chat-header h2 { margin:0; font-size:1.1rem; }
    .subtitle { margin:0; font-size:.75rem; opacity:.85; }
    .header-actions { display:flex; gap:.75rem; align-items:center; }
    .clear-btn, .back-btn { padding:.375rem .75rem; border-radius:4px; cursor:pointer; font-size:.85rem;
                             background:rgba(255,255,255,.15); color:white; border:1px solid rgba(255,255,255,.4);
                             text-decoration:none; }
    .clear-btn:hover, .back-btn:hover { background:rgba(255,255,255,.25); }

    /* Messages */
    .messages-container { flex:1; overflow-y:auto; padding:1rem 1.5rem; background:#f5f5f5; }
    .message-list { display:flex; flex-direction:column; gap:1rem; }

    /* Welcome */
    .welcome { text-align:center; padding:3rem 1rem; }
    .welcome-icon { font-size:3rem; margin-bottom:1rem; }
    .welcome h3 { margin:0 0 .5rem; color:#1976d2; font-size:1.4rem; }
    .welcome p { color:#666; margin-bottom:1.5rem; }
    .quick-prompts { display:flex; flex-wrap:wrap; gap:.5rem; justify-content:center; }
    .quick-prompts button { padding:.5rem 1rem; border:2px solid #1976d2; background:white; color:#1976d2;
                             border-radius:20px; cursor:pointer; font-size:.875rem; transition:.2s; }
    .quick-prompts button:hover { background:#1976d2; color:white; }

    /* Message bubbles */
    .message { display:flex; gap:.75rem; align-items:flex-start; }
    .user-message { flex-direction:row-reverse; }
    .message-avatar { font-size:1.5rem; flex-shrink:0; }
    .message-body { max-width:75%; }
    .message-content { padding:.75rem 1rem; border-radius:12px; line-height:1.5; font-size:.9rem; white-space:pre-wrap; }
    .user-message .message-content { background:#1976d2; color:white; border-radius:12px 0 12px 12px; }
    .assistant-message .message-content { background:white; border:1px solid #e0e0e0; border-radius:0 12px 12px 12px; color:#333; }
    .user-message .message-body { align-items:flex-end; }
    .message-time { font-size:.7rem; color:#999; margin-top:.25rem; text-align:right; }
    .user-message .message-time { text-align:right; }
    .assistant-message .message-time { text-align:left; }

    /* Typing indicator */
    .typing-indicator { display:flex; gap:4px; align-items:center; padding:.75rem 1rem;
                        background:white; border:1px solid #e0e0e0; border-radius:0 12px 12px 12px; width:60px; }
    .typing-indicator span { width:8px; height:8px; background:#1976d2; border-radius:50%;
                              animation:typing 1.2s infinite; }
    .typing-indicator span:nth-child(2) { animation-delay:.2s; }
    .typing-indicator span:nth-child(3) { animation-delay:.4s; }
    @keyframes typing { 0%,60%,100% { transform:translateY(0); } 30% { transform:translateY(-8px); } }

    /* Suggestions */
    .suggestions { display:flex; gap:.5rem; padding:.75rem 1.5rem; overflow-x:auto; background:#fff; border-top:1px solid #f0f0f0; }
    .suggestion-chip { white-space:nowrap; padding:.375rem .875rem; border:1px solid #1976d2; background:white;
                       color:#1976d2; border-radius:16px; cursor:pointer; font-size:.8rem; transition:.2s; flex-shrink:0; }
    .suggestion-chip:hover { background:#1976d2; color:white; }

    /* Input */
    .input-area { display:flex; gap:.75rem; padding:1rem 1.5rem; border-top:1px solid #e0e0e0; background:#fff; }
    .message-input { flex:1; padding:.75rem 1rem; border:1px solid #ddd; border-radius:8px; font-family:inherit;
                     font-size:.9rem; resize:none; line-height:1.4; min-height:44px; max-height:120px;
                     transition:.2s; }
    .message-input:focus { outline:none; border-color:#1976d2; box-shadow:0 0 0 2px rgba(25,118,210,.1); }
    .message-input:disabled { background:#f5f5f5; }
    .send-btn { width:48px; height:48px; border-radius:8px; border:none; background:#1976d2; color:white;
                font-size:1.25rem; cursor:pointer; flex-shrink:0; transition:.2s; }
    .send-btn:hover:not(:disabled) { background:#1565c0; }
    .send-btn:disabled { opacity:.5; cursor:not-allowed; }
    .input-hint { text-align:center; font-size:.7rem; color:#bbb; padding:.25rem; }
  `]
})
export class AiChatComponent implements AfterViewChecked, OnInit {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  private aiService = inject(AiService);
  private auth = inject(AuthService);

  messages = signal<ChatMessage[]>([]);
  loading = signal(false);
  suggestions = signal<string[]>([]);
  inputMessage = '';
  private conversationId: string | undefined;

  quickPrompts = [
    'What can I book here?',
    'Find me a flight to London tomorrow',
    'Show available hotel rooms in NYC this weekend',
    'Any cinema seats available today?',
    'How do I cancel a booking?'
  ];

  ngOnInit(): void {}

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  sendMessage(): void {
    const text = this.inputMessage.trim();
    if (!text || this.loading()) return;

    this.inputMessage = '';
    this.addMessage('user', text);
    this.loading.set(true);
    this.suggestions.set([]);

    const userId = this.auth.currentUser()?.id;

    this.aiService.chat(text, this.conversationId, userId).subscribe({
      next: (res: ChatResponse) => {
        this.conversationId = res.conversationId;
        this.addMessage('assistant', res.response);
        this.suggestions.set(res.suggestedActions || []);
        this.loading.set(false);
      },
      error: () => {
        this.addMessage('assistant', 'Sorry, I ran into an issue. Please try again.');
        this.loading.set(false);
      }
    });
  }

  sendQuickPrompt(prompt: string): void {
    this.inputMessage = prompt;
    this.sendMessage();
  }

  onEnterKey(event: Event): void {
    const keyEvent = event as KeyboardEvent;
    if (!keyEvent.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  clearConversation(): void {
    this.messages.set([]);
    this.conversationId = undefined;
    this.suggestions.set([]);
  }

  formatMessage(content: string): string {
    // Convert markdown-like bold, bullets, and newlines to HTML
    return content
      .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
      .replace(/\*([^*]+)\*/g, '<em>$1</em>')
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/\n/g, '<br>');
  }

  private addMessage(role: 'user' | 'assistant', content: string): void {
    this.messages.update(msgs => [...msgs, { role, content, timestamp: new Date() }]);
  }

  private scrollToBottom(): void {
    try {
      const el = this.messagesContainer?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch {}
  }
}
