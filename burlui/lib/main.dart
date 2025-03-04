/********************************************************************************/
/*                                                                              */
/*              main.dart                                                       */
/*                                                                              */
/*      Main program for BURL user interface                                    */
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

import 'package:flutter/material.dart';
import 'pages/splashpage.dart';

void main() {
  runApp(const MaterialApp(title: "BURL", home: SplashPage()));
}
