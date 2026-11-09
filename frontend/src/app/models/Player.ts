export interface Player {
    id?: number;
    username: string;
    nom?: string;
    email?: string;
    password?: string;
    busy?: boolean;
    credit?: number; 
}

export interface AuthResponse {
    token: string;
    playerId: number;
    username: string;
}