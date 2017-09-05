TERMUX_PKG_HOMEPAGE=https://www.metasploit.com/
TERMUX_PKG_DESCRIPTION="framework for pentesting"
TERMUX_PKG_VERSION=4.16.2
TERMUX_PKG_SRCURL=https://github.com/rapid7/metasploit-framework/archive/${TERMUX_PKG_VERSION}.tar.gz
TERMUX_PKG_SHA256=564072e633da3243252c3eb2cd005e406c005e0e4bbff56b22f7ae0640a3ee34
TERMUX_PKG_FOLDERNAME=metasploit-framework-$TERMUX_PKG_VERSION
TERMUX_PKG_BUILD_IN_SRC=yes

termux_step_configure () {
	return
}

termux_step_make () {
	return
}

termux_step_post_massage() {
        gem install --install-dir TERMUX_PREFIX/lib/ruby/gems/2.4.0 bundler
	
        gem install nokogiri -- --use-system-libraries --install-dir $TERMUX_PREFIX/lib/ruby/gems/2.4.0
        gem unpack grpc -v 1.4.1
        cd grpc-1.4.1
        patch -p1 < extconf.patch
        gem build grpc.gemspec
        gem install grpc-1.4.1.gem
        cd ..
        rm -r grpc-1.4.1


        cd $TERMUX_PREFIX/share/metasploit-framework
        bundle install -j5

        ln -s $TERMUX_PREFIX/share/metasploit-framework/msfconsole /data/data/com.termux/files/usr/bin/
} 
