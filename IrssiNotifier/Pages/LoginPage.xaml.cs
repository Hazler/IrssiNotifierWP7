﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using Microsoft.Phone.Controls;
using Microsoft.Phone.Shell;
using IrssiNotifier.Views;

namespace IrssiNotifier.Pages
{
	public partial class LoginPage
	{
		public LoginPage()
		{
			InitializeComponent();
			contentBorder.Child = new LoginView(this);
		}
	}
}