import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

export interface ChatResponse {
  response: string;
  conversationId: string;
  suggestedActions: string[];
}

export interface RecommendationRequest {
  bookingType: string;
  date?: string;
  preferences?: string;
  budget?: string;
}

export interface RecommendationResponse {
  summary: string;
  tips: string[];
  bestTimeToBook: string;
  estimatedPriceRange: string;
  topPicks: string[];
}

@Injectable({ providedIn: 'root' })
export class AiService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/ai`;

  chat(message: string, conversationId?: string, userId?: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.apiUrl}/chat`, {
      message,
      conversationId,
      userId
    });
  }

  getRecommendations(req: RecommendationRequest): Observable<RecommendationResponse> {
    return this.http.post<RecommendationResponse>(`${this.apiUrl}/recommendations`, req);
  }

  quickInsight(question: string): Observable<ChatResponse> {
    return this.http.get<ChatResponse>(`${this.apiUrl}/insight`, {
      params: { question }
    });
  }
}
