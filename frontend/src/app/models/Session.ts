import { Player } from "./Player";

export interface Session {
    id : number ;
    token : string;
    isActive : boolean;
    player : Player;

}