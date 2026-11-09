export interface Mail {
    id?: number;
    subject: string;
    content?: string;
    sender: string;
    sentDate: string;
    isRead: boolean;
}