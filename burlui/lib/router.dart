/********************************************************************************/
/*                                                                              */
/*              router.dart                                                     */
/*                                                                              */
/*      Define the routes for BURL                                              */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2025 Brown University -- Steven P. Reiss                      */
/********************************************************************************/
/*********************************************************************************
 *                                                                               *
 *  This work is licensed under Creative Commons Attribution-NonCommercial 4.0   *
 *  International.  To view a copy of this license, visit                        *
 *      https://creativecommons.org/licenses/by-nc/4.0/                          *
 *                                                                               *
 ********************************************************************************/

import 'package:go_router/go_router.dart';
import 'package:flutter/material.dart';
import 'package:string_validator/string_validator.dart';
import 'pages/splashpage.dart';
import 'pages/homepage.dart';
import 'pages/forgotpasswordpage.dart';
import 'pages/loginpage.dart';
import 'pages/registerpage.dart';

final GoRouter router = GoRouter(
  routes: [
    GoRoute(path: '/', builder: (context, state) => SplashPage()),
    GoRoute(path: '/home/:initial', builder: _homeBuilder),
    GoRoute(path: '/library/:library', builder: _libraryBuilder),
    GoRoute(path: "/entry/:entry", builder: _entryBuilder),
    GoRoute(path: '/forgotpassword', builder: _forgotPasswordBuilder),
    GoRoute(path: '/login', builder: _loginBuilder),
    GoRoute(path: '/register', builder: _registerBuilder),
  ],
);

Widget _homeBuilder(BuildContext context, GoRouterState state) {
  String init = state.pathParameters['initial'] ?? "";
  return BurlHomePage(toBoolean(init));
}

Widget _libraryBuilder(BuildContext context, GoRouterState state) {
  String? libname = state.pathParameters['library'];
  if (libname == null) return SplashPage();
  // construct lib data from name
  return SplashPage();
}

Widget _entryBuilder(BuildContext context, GoRouterState state) {
  String? ent = state.pathParameters['entry'];
  if (ent == null) return SplashPage();
  // construct entry data from id
  return SplashPage();
}

Widget _forgotPasswordBuilder(
  BuildContext context,
  GoRouterState state,
) {
  return BurlForgotPasswordWidget();
}

Widget _loginBuilder(BuildContext context, GoRouterState state) {
  return BurlLoginWidget();
}

Widget _registerBuilder(BuildContext context, GoRouterState state) {
  return BurlRegisterWidget();
}
