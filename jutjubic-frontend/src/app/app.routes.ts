import { Routes } from '@angular/router';
import { LayoutComponent } from './components/layout/layout.component';
import { HomeComponent } from './components/home/home.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { ActivateComponent } from './components/activate/activate.component';
import { WatchComponent } from './components/watch/watch.component';
import { TrendingComponent } from './components/trending/trending.component';
import { authGuard,guestGuard } from './guards/auth.guard';
import { CreateVideoComponent } from './components/createVideo/createVideo.component';
import { ProfileComponent } from './components/profile/profile.component';
import { MapComponent } from './components/map/map.component';
import { WatchPartyComponent } from './components/watch-party/watch-party.component';
import { WatchPartyRoomComponent } from './components/watch-party-room/watch-party-room.component';
import { WatchPartyJoinComponent } from './components/watch-party-join/watch-party-join.component';

export const routes: Routes = [
  {
    path: '',
    component: LayoutComponent,
    children: [
      {
        path: '',
        component: HomeComponent
      },
      {
        path: 'create-video',
        component: CreateVideoComponent,
        canActivate: [authGuard]
      },
      {
        path: 'watch/:id',
        component: WatchComponent,
      },
      {
        path: 'trending',
        component: TrendingComponent,
      },
      {
        path: 'map',
        component: MapComponent,
      },
      {
        path: 'watch-party',
        component: WatchPartyComponent,
      },
      {
        path: 'watch-party/room/:roomId',
        component: WatchPartyRoomComponent,
      },
      {
        path: 'watch-party/join/:inviteCode',
        component: WatchPartyJoinComponent,
      },
      {
        path: 'users/:username',
        component: ProfileComponent
      },
    ],
  },
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [guestGuard],
  },
  {
    path: 'register',
    component: RegisterComponent,
    canActivate: [guestGuard],
  },
  {
    path: 'activate',
    component: ActivateComponent,
  },
  {
    path: '**',
    redirectTo: '',
  },
];

