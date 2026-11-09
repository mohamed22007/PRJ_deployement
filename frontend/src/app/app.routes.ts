import { Component } from '@angular/core';
import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login';
import { HomeComponent } from './components/home/home';
import { GameComponent } from './components/game/game';
import { Profile } from './components/profile/profile';
import { Histoire } from './components/histoire/histoire';
import { Boitemail } from './components/boitemail/boitemail';
import { Inscription } from './components/inscription/inscription';
import { Regles } from './components/regles/regles';

export const routes: Routes =[
    { 
        path: 'auth', 
        children: [
            { path: 'login', component: LoginComponent },
            { path: 'inscription', component: Inscription },
            { path: '' , redirectTo: 'login' , pathMatch: 'full'}
        ]
    },
    { 
        path: 'home', 
        component: HomeComponent, // Ce composant aura la barre de menu
        children: [
        { path: 'profile', component: Profile },
        { path: 'game', component: GameComponent },
        { path: 'histoire', component : Histoire},
        { path: 'boitemail', component : Boitemail},
        { path: 'regles', component: Regles },
        { path: '', redirectTo: 'profile', pathMatch: 'full' }
        ]
    },
    { path: '', redirectTo: 'auth' ,pathMatch: 'full'}
]